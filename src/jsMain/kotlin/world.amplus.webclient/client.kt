package world.amplus.webclient

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.serialization.decodeFromHexString
import kotlinx.serialization.encodeToHexString
import kotlinx.serialization.protobuf.ProtoBuf
import org.w3c.dom.MessageEvent
import org.w3c.dom.WebSocket
import world.amplus.common.FromClient
import world.amplus.common.FromServer
import world.amplus.common.SType
import kotlin.js.Date

fun main() {
    window.onload = fun(evt) {
        setupSocket()
    }
}
var ws :WebSocket? = null

fun url() :String {
    println("Original href ${window.location.href}")
    var rtn = window.location.href.replace("https", "wss")
    rtn = rtn.replace("http","ws")
    rtn += "socket"
    println("I think the websocket will be at $rtn")
    return rtn
}
fun setupSocket() {

    val url = url()
    val lws = WebSocket(url)
    lws.onclose = fun (evt) {
        msg("Web socket closed $evt")
    }
    lws.onerror = fun (evt) {
        msg("web socket error $evt")
    }
    lws.onopen = fun (evt) {
        msg("web socket opened!! $evt")
        window.setInterval({ firePing() }, 1000)
    }
    lws.onmessage = fun(msg:MessageEvent) {
        println("Message type: ${msg.data}")
        val data = msg.data.toString()
        val fs = ProtoBuf.decodeFromHexString<FromServer>(data)
        when (fs.type) {
            SType.PONG -> {
                val delta = fs.pong!!.time -  Date.now()
                msg("Ping ${delta}ms")

            }
            SType.TIME -> TODO()
        }
    }
    ws = lws
}
var lastPingSent =0.toDouble()
fun firePing() {
    lastPingSent = Date.now()
    val fc = FromClient.ping(lastPingSent)
    val msg = ProtoBuf.encodeToHexString(fc)
    ws!!.send(msg)
}

fun msg(msg :String) {
    val root = document.getElementById("root")
    root?.innerHTML = msg
}
