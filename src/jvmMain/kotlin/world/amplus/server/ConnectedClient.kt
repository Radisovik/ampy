package world.amplus.server

import io.ktor.http.cio.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.encodeToHexString
import kotlinx.serialization.protobuf.ProtoBuf
import world.amplus.common.CType
import world.amplus.common.FromClient
import world.amplus.common.FromServer

class ConnectedClient(val ws: DefaultWebSocketServerSession) {
    suspend fun process(fc: FromClient) {
        when (fc.type) {
            CType.PING -> {
                val pong = FromServer.pong(fc.ping!!.time)
                ws.send(ProtoBuf.encodeToHexString(pong))
            }
        }
    }
}