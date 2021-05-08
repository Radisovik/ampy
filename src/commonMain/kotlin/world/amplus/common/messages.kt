package world.amplus.common

import kotlinx.serialization.*

@Serializable
enum class SType {
    PONG, TIME
}

@Serializable
data class Data(val a: Int, val b: String)


@Serializable
enum class CType {
    PING
}

@Serializable
data class FromServer(val type : SType) {
    var pong: Pong? = null
    companion object {
        fun pong(id: Double) = FromServer(SType.PONG).apply { pong = Pong(id) }
    }
}

@Serializable
data class FromClient(val type: CType) {
    var ping: Ping? = null
    companion object {
        fun ping(time: Double): FromClient {
            val mfc = FromClient(CType.PING)
            mfc.ping = Ping(time)
            return mfc
        }
    }
}

@Serializable
data class Pong(val time: Double)

@Serializable
data class Ping(val time: Double)
