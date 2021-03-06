package world.amplus.webclient

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.dom.createElement
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToHexString
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import org.w3c.dom.MessageEvent
import org.w3c.dom.WebSocket
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventListener
import world.amplus.common.*
import kotlin.experimental.and
import kotlin.js.Date


val game :Game by lazy {
    Game()
}

fun main() {
    window.asDynamic()["setupSocket"] = ::setupSocket
}

var ws :WebSocket? = null
var connected = false;

fun url() :String {
    println("Original href ${window.location.href}")
    if (window.location.hostname.contains("amplus.world")) {
        return "wss://amplus.world/socket"
    } else {
        return "ws://localhost:9000/socket"
    }
}


fun setupSocket(google_id_token:String?) {

    val url = url()
    val lws = WebSocket(url)
    lws.onclose = fun (evt) {
        game.chat("Disconnected: Closed")
        msg("Web socket closed $evt")
        connected=false
    }
    lws.onerror = fun (evt) {
        game.chat("Disconnected: Error")
        msg("web socket error $evt")
        connected=false
    }
    lws.onopen = fun (evt) {
        game.chat("Connected")
        val mfc = if (google_id_token ==null || google_id_token.isEmpty()) {
            FromClient.loginRequest("", true)
        } else  {
            FromClient.loginRequest(google_id_token, false)
        }
        lws.send(mfc.encode())

        window.setInterval({ firePing() }, 1000)
        connected=true
    }
    var animationStarted = 0
    lws.onmessage = fun(msg:MessageEvent) {
       // println("Message type: ${msg.data}")
        val data = msg.data.toString()
        val fs = Json.decodeFromString<FromServer>(data)

        if(fs.type!=SType.PONG) {
            println("Recv: ${fs.type}")
        }

        when (fs.type) {
            SType.PONG -> {
                val delta = Date.now() - fs.pong!!.time
                document.getElementById("ping")?.innerHTML = "Ping ${delta}ms"
            }
            SType.TIME -> TODO()
            SType.TERRAIN_UPDATE -> {
                processTerrain(fs.terrainUpdate!!)
                val welcomeBox = document.getElementById("welcome")!!
                welcomeBox.setAttribute("style", "visibility: hidden")
                if(animationStarted==0) {
                    println("we got at least one TU -- lets turn animation on")
                    game.chat("$animationStarted Start animation")
                    animationStarted++
                    game.animate() // start animation/movment..etc!
                }
            }

            SType.PLAYER_MOVED -> {
                val pm = fs.playerMoved
                if (pm == null) {
                    game.chat("Got a bad player moved!!")
                } else {
                    game.playerMoved(pm)
                }
            }
            SType.LOGIN_RESPONSE -> {
                val lr = fs.loginResponse!!
                val welcomeBox = document.getElementById("welcome")!!
                welcomeBox.innerHTML = "Welcome to the game ${lr.characterName} waiting for terrain to load"
                welcomeBox.setAttribute("style", "visibility: visible")
                game.camera.position.set(lr.youAreAt.x, lr.youAreAt.y, lr.youAreAt.z)
                game.maybeSendPosition(Date.now()) // send our initial position.. so we get subscribed terrain!

            }
            SType.ASK_FOR_NAME -> {
                val afn = fs.askForName!!
                val element = document.getElementById("prompt")!!
                element.setAttribute("placeholder", afn.details)
                //element.asDynamic().defaultValue=afn.details
             //   element.asDynamic().style.dispaly="block"
                element.setAttribute("style", "visibility: visible")
                element.addEventListener("change", userNamePicker)
            }
            SType.CHAT_MSG -> {
                val cm = fs.chatMsg!!
                game.chat("${cm.from}> ${cm.msg}")
            }
            SType.YOU_AT -> {
                val at = fs.youAreAt!!
                game.camera.position.set(at.at.x, at.at.y, at.at.z)
                val o = at.orientation
                game.camera.quaternion.set(o.x, o.y, o.z, o.w)
            }
        }
    }
    ws = lws
}

private val userNamePicker : EventListener = object : EventListener {
    override fun handleEvent(event: Event) {
        val element = document.getElementById("prompt")!!
        val fc = FromClient.pickedName(element.asDynamic().value)
        val efc = fc.encode()
        ws?.send(efc)
        element.asDynamic().style.dispaly="none"
        element.setAttribute("style", "visibility: hidden")
        element.removeEventListener("change", this)
        element.asDynamic().focus()
    }
}

 fun FromClient.encode(): String {
    return ProtoBuf.encodeToHexString(this)
}


data class ExposedSide(val value: Long, val tid: Int) {
    /**
     *   return x.toLong().shl(16).or(y.toLong().and(0xffff)).shl(16).or(z.toLong().and(0xffff)).shl(8)
    .or(side.mask.toLong().and(0xff))
     *  // in hex it would be
     *  7 6 5 4 3 2 1 0
     *  XXXXYYYYZZZZSSSS
     */
    fun x(): Short {
        return value.shr(48).toShort()
    }

    fun y(): Short {
        return value.shr(32).toShort()
    }

    fun z(): Short {
        return value.shr(16).toShort()
    }

    fun side(): Short {
        return value.and(0xffff).toShort()
    }

}


fun ek(x: Short, y: Short, z: Short, side: Short): Long {
    var rtn = 0L
    rtn += (x.and(0xffff.toShort()))
    rtn = rtn.shl(16)
    rtn += (y.and(0xffff.toShort()))
    rtn = rtn.shl(16)
    rtn += (z.and(0xffff.toShort()))
    rtn = rtn.shl(16)
    rtn += (side.and(0xff))
    return rtn
}

val CHUNK_SIZE=16

var chunks = mutableMapOf<ChunkShortName, ChunkData>()

fun processTerrain(tu: TerrainUpdates) {
   // println("Exciting we got a terrain update! ${tu}")
    val key = tu.chunkName
    chunks.getOrPut(key) { ChunkData(key) }.process(tu)


}

var lastPingSent =0.toDouble()
fun firePing() {
    lastPingSent = Date.now()
    val fc = FromClient.ping(lastPingSent)
    val msg = ProtoBuf.encodeToHexString(fc)
    ws!!.send(msg)
}

fun msg(msg :String) {
    println(msg)
}
