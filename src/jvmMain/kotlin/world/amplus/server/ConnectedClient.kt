package world.amplus.server

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.gson.*
import com.mongodb.client.model.Filters
import io.ktor.http.cio.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encodeToHexString
import kotlinx.serialization.protobuf.ProtoBuf
import org.bson.Document
import world.amplus.common.*
import world.amplus.server.dmo.PlayerData
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import java.util.stream.Collectors





class ConnectedClient(private val wss: DefaultWebSocketServerSession, val connectionNumber: Int) :BlockStoreListener {
    private var idToken: GoogleIdToken?=null
    private lateinit var email: String
    var currentWorld : String = "root"
    var currentChunk = ChunkName()
    val subscribedTo = HashSet<ChunkName>()
    val bs = BlockStores.blockStore(currentWorld)
    val sendQueue = ArrayBlockingQueue<String>(512)
    private lateinit var playerData : PlayerData
    var playerName = ""
    var playerUid = ""
    var logger :Logger = Logger.getLogger("Connection $connectionNumber")
    val latestKnown = ConcurrentHashMap<ChunkName, Int>()
    var readyToPlay = false


    init {
        GlobalScope.launch {
            while(true) {
                val msg = sendQueue.take()
                if (msg == "die") {
                    break
                }
                wss.send(msg)
            }
        }
    }
    companion object {
        val connectedClients = ConcurrentHashMap<String, ConnectedClient>()
        val transport = NetHttpTransport.Builder().build()
        val verifier = GoogleIdTokenVerifier.Builder(transport, GsonFactory())
            .setAudience(Collections.singletonList("402173895467-6qa2efadumtv82ks1e9cfmglhq07n0j3.apps.googleusercontent.com"))
            .build()
        val random = java.util.Random(System.currentTimeMillis())

    }

    fun send(fromServer:FromServer) {
        send(fromServer.encodeToString())
    }

    fun send(fromServer: String) {
        if (!sendQueue.offer(fromServer, 5, TimeUnit.SECONDS)) {
            logger.severe("SendQueue blocked for 5 seconds. Dropped message")
        }
    }

    suspend fun die() {
        logger.info("Cleaning up $playerName")
        connectedClients.remove(this.playerName)
        subscribedTo.forEach {
            bs.unsubscribe(it, this)
        }
    }

    suspend fun process(fc: FromClient) {
        when (fc.type) {
            CType.LOGIN_REQUEST -> {
                val lr = fc.loginRequest!!
                if (lr.anonymous) {
                    val uid = PlayerData.nextUid()
                    playerName = "Unknown-%g${uid}"
                    val resp = FromServer.loginResponse(playerName, V3f(1f, 2f, 1f), V4f(1f,1f,1f,1f))
                    send(resp)
                    assignName(playerName, uid)
                } else {
                    handleGoogleUser(lr)
                }
            }
            CType.PING -> {
                if (readyToPlay) {
                    val pong = FromServer.pong(fc.ping!!.time)
                    send(pong.encodeToString())
                }
            }
            CType.IAMAT -> {
                if (readyToPlay) {
                    val iat = fc.iamiat!!
                    val pm =
                        FromServer.playerMoved(playerName, iat.position, iat.orientation, System.currentTimeMillis())
                    val msg = pm.encodeToString()
                    for (it in connectedClients.entries) {
                        if (it.key != playerName) {
                            it.value.send(msg)
                        }
                    }

                    playerData.updatePosition(iat.position, iat.orientation)

                    val nextChunk = ChunkName(currentWorld, iat.position.x, iat.position.z)
                    if (nextChunk != currentChunk) {

                        val newChunks = nextChunk.myNeightbors(5)

                        val toAdd = newChunks.stream()
                            .filter { !subscribedTo.contains(it) }
                            .sorted { o1, o2 -> o1.distanceTo(nextChunk).compareTo(o2.distanceTo(nextChunk)) }
                            .collect(Collectors.toList())

                        val toRemove = subscribedTo.stream()
                            .filter { !newChunks.contains(it) }
                            .collect(Collectors.toSet())


                        var avoided = 0
                        for (chunkName in toAdd) {
                            subscribedTo.add(chunkName)
                            val lk = latestKnown.getOrPut(chunkName) { -1 }
                            if (!bs.subscribe(chunkName, lk, this)) {
                                avoided++
                            }
                        }

                        for (chunkName in toRemove) {
                            subscribedTo.remove(chunkName)
                            bs.unsubscribe(chunkName, this)
                        }
                        logger.info("Subscribed [${toAdd.size}/$avoided].  Unsubscribed [${toRemove.size}]")


                        currentChunk = nextChunk
                    }
                }
            }
            CType.TOOLUSE -> {
                if (readyToPlay) {
                    val tu = fc.toolUse!!
                    if (tu.tool == 192) {
                        bs.remove(tu.start, tu.end)
                    } else {
                        val bt = BlockType.values()[tu.tool - 48]
                        logger.info("Asked to use tool ${tu.tool} which would be ${tu.tool - 48} and that is a $bt")
                        bs.put(tu.start, tu.end, bt)
                    }
                }
            }
            CType.PICKED_PLAYER_NAME -> {
                val ppn = fc.pickedPlayerName!!
                if (idToken== null) {
                    logger.severe("Sorry only google signed in users can pick a name")
                } else {

                    logger.info("oo player would like to be ${ppn.requestedName}")
                    // first check to make sure the name isn't already in use
                    val otherPlayer = PlayerData.byName(ppn.requestedName)

                    if (otherPlayer != null) {
                        logger.warning("That player name already exists")
                        val mfs = FromServer.askForName("That name is already in use; please pick another")
                        send(mfs)
                    } else { // cool the name is not in use

                        logger.info("${ppn.requestedName} does not look taken.. lets save it!")
                        val pd = PlayerData.byEmail(email)!!
                        val npd = pd.copy(playerName = ppn.requestedName)
                        PlayerData.savePlayer(npd)

                        val lr = FromServer.loginResponse(ppn.requestedName, V3f(1f, 1f, 1f), V4f(1f,1f,1f,1f))
                        send(lr)
                        logger.info("${ppn.requestedName} was saved and our player now has a cool name")
                        assignName(ppn.requestedName, email)
                    }
                }
            }
            CType.ISAID -> {
                if (readyToPlay) {
                    val chatText = fc.iSaid!!
                    handleChatText(chatText)
                }
            }
        }
    }



    private fun handleChatText(chatText: ISaid) {
        val cm = ChatMessage("", playerName, chatText.msg)
        broadcast(cm)
    }


    val gson = Gson()
    private fun handleGoogleUser(request:  LoginRequest) {
        val idTokenText = request.id_token
        val lidToken = verifier.verify(idTokenText)
        if (lidToken != null) {
            val payload = lidToken.payload
            val name = payload.getValue("name").toString()
            val givenName = payload.getValue("given_name").toString()
            email = payload.getValue("email").toString()

            val pd = PlayerData.byUid(email)
            if(pd == null || pd.playerName=="") {
                val pd = PlayerData(email)
                logger.info("Adding a player record for: $email")
                PlayerData.savePlayer(pd)
                logger.info("$email <-- we need a playername!")
                val mfs = FromServer.askForName("Please supply a player name")
                send(mfs)
            } else {
                logger.info("..oo the player record is ${gson.toJson(pd)}")
                val playerData = PlayerData.byEmail(email)!!
                val resp =FromServer.loginResponse(playerData.playerName, playerData.location, playerData.orientation)
                send(resp)
                assignName(playerData.playerName, email)
            }
            idToken = lidToken
        } else {
            logger.severe("That is not a valid token!")
        }
    }

    private fun broadcast(cm:ChatMessage) {
        val mfs = FromServer.chatMessage(cm)
        val smfs = mfs.encodeToString()
        for (cc in connectedClients.values) {
            cc.send(smfs)
        }
    }

    private fun serverSays(s: String) {
        val cm = ChatMessage("", "Server", s)
        broadcast(cm)
    }

    @Synchronized
    fun newer(chunkName: ChunkName, tv : Int) :Boolean {
        val lk = latestKnown.getOrPut(chunkName) { -1}
        if (lk !=tv ) {
            latestKnown[chunkName] = tv
            return true
        } else {
            return false
        }
    }

    override fun patchChange(chunkName: ChunkName, addTheseFaces: List<Long>,
        textures: List<Int>, removeFaces: List<Long>, version: Int) {
        val csn = ChunkShortName(chunkName.cx, chunkName.cz)
        val tu = TerrainUpdates(csn, addTheseFaces, textures, removeFaces)
        val mfs = FromServer.terrainUpdate(tu)
        // uncomment to get info on terrain patch sizes for the various formats
        // typical full flat chunk is 9426,10930,5465...
        // so a pb hex string is BIGGER then the JSON.. but a pb byte array would be 50 better
        // someday we'll get there..  but I have problems understanding websocket byte arrays
//        logger.info(
//            "TPSize: ${mfs.encodeToString().length} pbhex ${ProtoBuf.encodeToHexString(mfs).length} ${
//                ProtoBuf.encodeToByteArray(
//                    mfs
//                ).size
//            }"
//        )
        latestKnown.put(chunkName, version)
        send(mfs.encodeToString())
    }
    private fun assignName(name:String, uid:String) {
        playerName = name
        playerUid = uid
        playerData = PlayerData.byUid(uid)!!
        connectedClients.put(name, this)
        logger = Logger.getLogger(name)
        serverSays("Welcome $name to the game!")
        readyToPlay = true
    }
}

val gson = Gson()

private fun FromServer.encodeToString() :String {
    val pbHex = ProtoBuf.encodeToHexString(this)
    val json = gson.toJson(this)
    val pb = ProtoBuf.encodeToByteArray(this)
    // return ProtoBuf.encodeToHexString(this)
   // println("Json size: ${json.length} vs. PB hex size: ${pbHex.length} vs. PB ${pb.size}")
    return json
}


private fun ChunkName.distanceTo(nextChunk: ChunkName): Int {
    return(distanceSquared(this.cx, this.cz, nextChunk.cx, nextChunk.cz))
}
