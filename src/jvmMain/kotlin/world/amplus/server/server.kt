package world.amplus.server

import world.amplus.common.FromClient
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.cio.websocket.*
import io.ktor.http.content.*
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.websocket.*
import kotlinx.html.*
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.ProtoBuf
import org.slf4j.event.Level
import java.io.File
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

enum class RUN_MODE(val port:Int, val srf:String) {
    DEV(9000,"."),PROD(9000,".")
}

val logger = Logger.getLogger("Server")

fun pickPort(): RUN_MODE {
    val osname = System.getProperty("os.name")

    val rtn = if (osname.contains("windows", true) ||
        osname.contains("Mac OS X", true)
    ) {
        RUN_MODE.DEV
    } else {
        RUN_MODE.PROD
    }
    logger.info("Running on $osname runmode: $rtn port:${rtn.port} root: ${rtn.srf}")
    return rtn
}

fun main() {
    System.setProperty("java.util.logging.SimpleFormatter.format",
        "%1\$tF %1\$tT [%4\$s] [%3\$s] %5\$s%6\$s%n")
    val logger = Logger.getLogger("Server")

    val runMode = pickPort()

    val si = File(String.format("${runMode.srf}/www/index.html"))
    if (!si.exists()) {
        logger.severe("index.html is missing! here is where I looked ${si.absolutePath}")
    }

    embeddedServer(Netty, port = runMode.port, host = "0.0.0.0") {
        install(WebSockets)
        install(CallLogging) {
            level = Level.INFO
        }
        routing {
            webSocket("/socket") { // websocketSession
                val cc = ConnectedClient(this)
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            val fc = ProtoBuf.decodeFromHexString<FromClient>(text)
                            cc.process(fc)
                        }
                        else -> {
                            logger.warning("Unknown frame type: $frame")
                        }
                    }
                }
                cc.die()
            }
            static("/") {
                staticRootFolder = File(runMode.srf)
                files("www")
                logger.info("Serving ${si.canonicalPath}")
                default("www/index.html")
            }
        }
    }.start(wait = true)
}
