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

class ConnectedClient(val ws: DefaultWebSocketServerSession) :BlockStoreListener {
    var currentWorld : String = "root"
    var currentChunk = ChunkName()
    val subscribedTo = HashSet<ChunkName>()
    val bs = BlockStores.blockStore(currentWorld)
    init {

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
        }
    }

    override fun patchChange(chunkName: ChunkName, addTheseFaces: List<Long>,
        textures: List<Int>, removeFaces: List<Long>, version: Int) {

        val csn = ChunkShortName(chunkName.cx, chunkName.cz)
        val tu = TerrainUpdates(csn, addTheseFaces, textures, removeFaces )
        val mfs = FromServer.terrainUpdate(tu)

        println("Sending terrain patch this big: ${mfs.encodeToString().length}")

        GlobalScope.launch {
            ws.send(mfs.encodeToString())
        }
    }
}

val gson = Gson()

private fun FromServer.encodeToString() :String {
   // return ProtoBuf.encodeToHexString(this)
    return gson.toJson(this)
}


private fun ChunkName.distanceTo(nextChunk: ChunkName): Int {
    return(distanceSquared(this.cx, this.cz, nextChunk.cx, nextChunk.cz))
}
