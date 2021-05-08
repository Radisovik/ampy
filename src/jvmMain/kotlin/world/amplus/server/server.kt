package world.amplus.server

import world.amplus.common.FromClient
import io.ktor.application.*
import io.ktor.html.respondHtml
import io.ktor.http.HttpStatusCode
import io.ktor.http.cio.websocket.*
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.websocket.*
import kotlinx.html.*
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.ProtoBuf
import world.amplus.common.CType
import world.amplus.common.FromServer
import java.util.logging.Logger


fun HTML.index() {
    head {
        title("Hello from Ktor!")
    }
    body {
        div {
            +"Hello from Ktor"
        }
        div {
            id = "root"
        }
        script(src = "/static/js.js") {}
    }
}

fun main() {
    val logger = Logger.getLogger("Server")
    embeddedServer(Netty, port = 8080, host = "127.0.0.1") {
        install(WebSockets)

        routing {
            get("/") {
                call.respondHtml(HttpStatusCode.OK, HTML::index)
            }
            static("/static") {
                resources()
            }
            webSocket("/socket") { // websocketSession
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            val fc = ProtoBuf.decodeFromHexString<FromClient>(text)
                            when (fc.type) {
                                CType.PING -> {
                                    val pong = FromServer.pong(fc.ping!!.time)
                                    this.send(ProtoBuf.encodeToHexString(pong))
                                }
                            }
                        }
                        else -> {
                            logger.warning("Unknown frame type: $frame")
                        }
                    }
                }
            }
        }
    }.start(wait = true)
}
