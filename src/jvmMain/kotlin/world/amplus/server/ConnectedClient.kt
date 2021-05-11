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
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger


var gid=0

val connectedClients = ConcurrentHashMap<String, ConnectedClient>()

class ConnectedClient(val ws: DefaultWebSocketServerSession) :BlockStoreListener {
    var currentWorld : String = "root"
    var currentChunk = ChunkName()
    val subscribedTo = HashSet<ChunkName>()
    val bs = BlockStores.blockStore(currentWorld)
    val playerName = "Unknown-${gid++}"
    val logger = Logger.getLogger(playerName)
    init {
        connectedClients.put(playerName, this)
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
                ws.send(pong.encodeToString())
            }
            CType.LOGIN_REQUEST -> TODO()
            CType.IAMAT -> {
                val iat = fc.iamiat!!
                val pm = FromServer.playerMoved(playerName, iat.v3i, System.currentTimeMillis())
                val msg = pm.encodeToString()
                for (it in connectedClients.entries) {
                    if (it.key != playerName) {
                        it.value.ws.send(msg)
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


                    for (chunkName in toAdd) {
                        logger.info("Add subscribe here: $chunkName")
                        subscribedTo.add(chunkName)
                        bs.subscribe(chunkName,-1, this)
                    }


                    for (chunkName in toRemove) {
                        logger.info("Remove subscribe here: $chunkName")
                        subscribedTo.remove(chunkName)
                        bs.unsubscribe(chunkName, this)
                    }


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

    override fun patchChange(chunkName: ChunkName, addTheseFaces: List<Long>,
        textures: List<Int>, removeFaces: List<Long>, version: Int) {

        val csn = ChunkShortName(chunkName.cx, chunkName.cz)
        val tu = TerrainUpdates(csn, addTheseFaces, textures, removeFaces )
        val mfs = FromServer.terrainUpdate(tu)

        logger.info("TPSize: ${mfs.encodeToString().length} pbhex ${ProtoBuf.encodeToHexString(mfs).length} ${ProtoBuf.encodeToByteArray(mfs).size}")

        GlobalScope.launch {
            ws.send(mfs.encodeToString())
        }
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
