package world.amplus.webclient

import ext.aspectRatio
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.serialization.decodeFromHexString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToHexString
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import org.w3c.dom.MessageEvent
import org.w3c.dom.WebSocket
import world.amplus.common.FromClient
import world.amplus.common.FromServer
import world.amplus.common.SType
import world.amplus.common.TerrainUpdates
import kotlin.experimental.and
import kotlin.js.Date



fun main() {
    Game().animate()
    window.onload = fun(evt) {
        setupSocket()
    }
}
var ws :WebSocket? = null
var connected = false;

fun url() :String {
    println("Original href ${window.location.href}")
    var rtn = window.location.href.replace("https", "wss")
    rtn = rtn.replace("http","ws")
    rtn += "socket"
    rtn = "ws://localhost:9000/socket"
    println("I think the websocket will be at $rtn")
    return rtn
}
fun setupSocket() {

    val url = url()
    val lws = WebSocket(url)
    lws.onclose = fun (evt) {
        msg("Web socket closed $evt")
        connected=false
    }
    lws.onerror = fun (evt) {
        msg("web socket error $evt")
        connected=false
    }
    lws.onopen = fun (evt) {
        msg("web socket opened!! $evt")
        window.setInterval({ firePing() }, 1000)
        connected=true
    }
    lws.onmessage = fun(msg:MessageEvent) {
       // println("Message type: ${msg.data}")
        val data = msg.data.toString()
        val fs = Json.decodeFromString<FromServer>(data)
        when (fs.type) {
            SType.PONG -> {
                val delta = Date.now() - fs.pong!!.time
                document.getElementById("ping")?.innerHTML = "Ping ${delta}ms"
            }
            SType.TIME -> TODO()
            SType.TERRAIN_UPDATE -> {
                processTerrain(fs.terrainUpdate!!)
            }
        }
    }
    ws = lws
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
fun processTerrain(tu: TerrainUpdates) {
    println("Exciting we got a terrain update! ${tu}")
    for (face in tu.addTheseFaces) {
        val es =ExposedSide(face,0)
        val l =ek(es.x(), es.y(),es.z(), es.side())

        if (l!=face) {
            println("buggg")
        } else {
            println("all good")
        }
    }


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
