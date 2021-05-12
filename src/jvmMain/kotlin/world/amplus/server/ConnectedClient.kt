package world.amplus.server

import io.ktor.http.cio.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToHexString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import world.amplus.common.*
import java.util.stream.Collectors
import com.google.gson.*
import kotlinx.serialization.encodeToByteArray
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.logging.Logger


var gid=0

val connectedClients = ConcurrentHashMap<String, ConnectedClient>()

class ConnectedClient(private val wss: DefaultWebSocketServerSession, val playerName: String) :BlockStoreListener {
    var currentWorld : String = "root"
    var currentChunk = ChunkName()
    val subscribedTo = HashSet<ChunkName>()
    val bs = BlockStores.blockStore(currentWorld)
    val sendQueue = ArrayBlockingQueue<String>(512)

    val logger = Logger.getLogger(playerName)
    val latestKnown = ConcurrentHashMap<ChunkName, Int>()
    init {
        val lr = FromServer.loginResponse(System.currentTimeMillis().toDouble(), playerName, V3f(2f,2f,2f))
        val msg = lr.encodeToString()
        send(msg)
        connectedClients.put(playerName, this)
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
            CType.PING -> {
                val pong = FromServer.pong(fc.ping!!.time)
                send(pong.encodeToString())
            }
            CType.IAMAT -> {
                val iat = fc.iamiat!!
                val pm = FromServer.playerMoved(playerName, iat.v3i, System.currentTimeMillis())
                val msg = pm.encodeToString()
                for (it in connectedClients.entries) {
                    if (it.key != playerName) {
                        it.value.send(msg)
                    }
                }

                val nextChunk = ChunkName(currentWorld, iat.v3i.x, iat.v3i.z)
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
                        val lk = latestKnown.getOrPut(chunkName){ -1}
                        if (!bs.subscribe(chunkName,lk, this)) {
                            avoided ++
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
            CType.TOOLUSE -> {
                val tu = fc.toolUse!!
                if (tu.tool == 192) {
                    bs.remove(tu.start, tu.end)
                } else {
                    val bt =BlockType.values()[tu.tool-48]
                    logger.info("Asked to use tool ${tu.tool} which would be ${tu.tool-48} and that is a $bt")
                    bs.put(tu.start, tu.end, bt)
                }
            }
        }
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
