package world.amplus.server

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import world.amplus.common.FromClient
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.http.content.*
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.html.*
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.ProtoBuf
import org.slf4j.event.Level
import java.io.File
import java.util.*
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

fun pickPort(args: Array<String>): RUN_MODE {
    for (arg in args) {
        if (arg == "devmode") {
            return RUN_MODE.DEV
        }
    }
    return RUN_MODE.PROD
}

fun main(args:Array<String>) {
    System.setProperty("java.util.logging.SimpleFormatter.format",
        "%1\$tF %1\$tT [%4\$s] [%3\$s] %5\$s%6\$s%n")
    val logger = Logger.getLogger("Server")

    val runMode = pickPort(args)

    val si = File(String.format("${runMode.srf}/www/index.html"))
    if (!si.exists()) {
        logger.severe("index.html is missing! here is where I looked ${si.absolutePath}")
    }

    val transport = NetHttpTransport.Builder().build()
    val verifier = GoogleIdTokenVerifier.Builder(transport, GsonFactory())
        .setAudience(Collections.singletonList("402173895467-6qa2efadumtv82ks1e9cfmglhq07n0j3.apps.googleusercontent.com"))
        .build()

    var gid=0

    embeddedServer(Netty, port = runMode.port, host = "0.0.0.0") {
        install(WebSockets)
        install(CallLogging) {
            level = Level.INFO
        }
        install(CachingHeaders)
        routing {
            webSocket("/socket") { // websocketSession

                val frame = incoming.receive() // get their login token

                if (frame is Frame.Text) {
                    val readText = frame.readText()
                    if (readText != "anonymous") {
                        val idToken = verifier.verify(readText)
                        if (idToken != null) {
                            val payload = idToken.payload
                            val name = payload.getValue("name").toString()
                            val givenName = payload.getValue("given_name").toString()
                            logger.info("name from token $name")
                            logger.info("given name $givenName")
                            processUser(this, incoming, name)
                        } else {
                            logger.severe("Failed to validate idtoken")
                        }
                    } else {
                        val playerName = "Unknown-${gid++}"
                        logger.info("Anonymous user assigned $playerName")
                        processUser(this, incoming, playerName)
                    }
                } else {
                    logger.severe("First web socket must be text")
                }
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

suspend fun processUser(ws: DefaultWebSocketServerSession, incoming: ReceiveChannel<Frame>, name: String) {
    val cc = ConnectedClient(ws, name)
    for (frame in incoming) {
        when (frame) {
            is Frame.Text -> {
                val text = frame.readText()
                val fc = ProtoBuf.decodeFromHexString<FromClient>(text)
                cc.process(fc)
            }
            else -> {
                logger.warning("Unknown frame type: $frame")
                cc.die()
                return
            }
        }
    }
    cc.die()
}
