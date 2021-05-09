package world.amplus.common

import kotlinx.serialization.*

@Serializable
enum class SType {
    PONG, TIME, LOGIN_RESPONSE
}


@Serializable
enum class CType {
    PING,LOGIN_REQUEST
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
data class V3i(val x: Int, val y: Int, val z: Int) {
    constructor() : this(Int.MIN_VALUE, Int.MIN_VALUE, Int.MIN_VALUE)
}

@Serializable
enum class Side(val mask: Byte, val delta: V3i, private val otherSideOrd: Int) {
    NX(0b00000001, V3i(-1, 0, 0), 1),
    PX(0b00000010, V3i(+1, 0, 0), 0),
    NY(0b00000100, V3i(0, -1, 0), 3),
    PY(0b00001000, V3i(0, +1, 0), 2),
    NZ(0b00010000, V3i(0, 0, -1), 5),
    PZ(0b00100000, V3i(0, 0, 0 + 1), 4);

    companion object {
        val byValue = mutableMapOf<Byte, Side>()

        init {
            values().forEach { byValue[it.mask] = it }
        }

        fun value(byte: Byte): Side {
            return byValue[byte]!!
        }
    }

    fun other() = values()[otherSideOrd]
}

@Serializable
data class LoginRequest(val time: Double)

@Serializable
data class LoginResponse(val time: Double)

@Serializable
data class Pong(val time: Double)

@Serializable
data class Ping(val time: Double)
