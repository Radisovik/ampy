package world.amplus.server

import io.ktor.http.cio.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.encodeToHexString
import kotlinx.serialization.protobuf.ProtoBuf
import world.amplus.common.CType
import world.amplus.common.FromClient
import world.amplus.common.FromServer
import java.util.stream.Collectors

class ConnectedClient(val ws: DefaultWebSocketServerSession) :BlockStoreListener {
    var currentWorld : String = "root"
    var currentChunk = ChunkName()
    val subscribedTo = HashSet<ChunkName>()
    val bs = BlockStores.blockStore(currentWorld)
    suspend fun process(fc: FromClient) {
        when (fc.type) {
            CType.PING -> {
                val pong = FromServer.pong(fc.ping!!.time)
                ws.send(ProtoBuf.encodeToHexString(pong))
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
                    }

                    for (chunkName in toRemove) {
                        logger.info("Reomve subscribe here: $chunkName")
                        subscribedTo.remove(chunkName)
                    }


                    currentChunk = nextChunk
                }
            }
        }
    }

    override fun patchChange(
        chunkName: ChunkName,
        addTheseFaces: List<Long>,
        textures: List<Int>,
        removeFaces: List<Long>,
        version: Int
    ) {
        TODO("Not yet implemented")
    }


}

private fun ChunkName.distanceTo(nextChunk: ChunkName): Int {
    return(distanceSquared(this.cx, this.cz, nextChunk.cx, nextChunk.cz))
}
