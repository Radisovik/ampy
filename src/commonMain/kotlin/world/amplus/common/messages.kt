package world.amplus.common

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
data class ChunkShortName(val cx: Int, val cz: Int)

@Serializable
enum class SType {
    PONG, TIME, LOGIN_RESPONSE, TERRAIN_UPDATE, PLAYER_MOVED,ASK_FOR_NAME,CHAT_MSG, YOU_AT
}

@Serializable
enum class CType {
    PING,LOGIN_REQUEST,IAMAT,TOOLUSE,PICKED_PLAYER_NAME,ISAID
}

@Serializable
data class PlayerMoved(val name: String, val position: V3f, val orientation: V4f, val asOf: Long)

@Serializable
data class FromServer(val type : SType) {
    var pong: Pong? = null
    var terrainUpdate: TerrainUpdates? = null
    var playerMoved: PlayerMoved?=null
    var loginResponse: LoginResponse?=null
    var askForName: AskCharacterName?=null
    var chatMsg: ChatMessage?=null
    var youAreAt: YouAreAt?=null
    companion object {
        fun pong(id: Double) = FromServer(SType.PONG).apply { pong = Pong(id) }
        fun terrainUpdate(tu:TerrainUpdates) = FromServer(SType.TERRAIN_UPDATE).apply { terrainUpdate = tu}
        fun playerMoved(name:String, pos:V3f, orientation: V4f,asOf:Long) = FromServer(SType.PLAYER_MOVED).apply { playerMoved = PlayerMoved(name, pos, orientation, asOf) }
        fun loginResponse(name:String, pos:V3f, orientation: V4f) = FromServer(SType.LOGIN_RESPONSE).apply { loginResponse = LoginResponse(name, pos, orientation)}
        fun askForName(details:String) = FromServer(SType.ASK_FOR_NAME).apply { askForName = AskCharacterName(details) }
        fun chatMessage(cm:ChatMessage) = FromServer(SType.CHAT_MSG).apply { chatMsg = cm }
        fun youAreAt(at:V3f, orientation: V4f) = FromServer(SType.YOU_AT).apply { youAreAt = YouAreAt (at, orientation) }
    }
}

@Serializable
data class FromClient(val type: CType) {
    var ping: Ping? = null
    var iamiat: IAmAt? = null
    var toolUse: ToolUse? = null
    var loginRequest: LoginRequest?=null
    var pickedPlayerName: PickedPlayerName?=null
    var iSaid: ISaid?=null

    companion object {
        fun isaid(msg:String):FromClient {
            return FromClient(CType.ISAID).apply { iSaid = ISaid(msg) }
        }
        fun ping(time: Double): FromClient {
            val mfc = FromClient(CType.PING)
            mfc.ping = Ping(time)
            return mfc
        }
        fun iamat(pos: V3f, orientation: V4f): FromClient {
            val mfc = FromClient(CType.IAMAT)
            mfc.iamiat = IAmAt(pos, orientation)
            return mfc
        }
        fun tooluse( tool:Int,  start:V3i,  end:V3i): FromClient {
            val mfc = FromClient(CType.TOOLUSE)
            mfc.toolUse = ToolUse(tool, start, end)
            return mfc
        }
        fun loginRequest( id_token:String, anonymous:Boolean): FromClient {
            val mfc = FromClient(CType.LOGIN_REQUEST)
            mfc.loginRequest = LoginRequest(id_token, anonymous)
            return mfc
        }
        fun pickedName(pickedName:String) :FromClient {
            return FromClient(CType.PICKED_PLAYER_NAME).apply {
                pickedPlayerName = PickedPlayerName(pickedName)
            }
        }
    }
}

@Serializable
data class YouAreAt(val at:V3f, val orientation: V4f)

@Serializable
data class ISaid(val msg:String)

@Serializable
data class ChatMessage(val destination:String, val from:String, val msg:String)

@Serializable
data class ToolUse(val tool:Int, val start:V3i, val end:V3i)

@Serializable
data class V4f(val x: Float, val y: Float, val z: Float, val w:Float) {
    constructor() : this(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE)
}

@Serializable
data class V3f(val x: Float, val y: Float, val z: Float) {
    constructor() : this(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE)
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

//        fun value(byte: Byte): Side {
//            return byValue[byte]!!
//        }
    }

    fun other() = values()[otherSideOrd]
}


//chunkName: ChunkName,
//addTheseFaces: List<Long>,
//textures: List<Int>,
//removeFaces: List<Long>,
//version: Int

@Serializable
data class TerrainUpdates(val chunkName: ChunkShortName, val addTheseFaces:List<Long>, val textures:List<Int>, val removeTheseFaces:List<Long>)

@Serializable
data class IAmAt(val position: V3f, val orientation: V4f)

@Serializable
data class LoginRequest(val id_token: String, val anonymous: Boolean)

@Serializable
data class AskCharacterName(val details:String)

@Serializable
data class PickedPlayerName(val requestedName:String)

@Serializable
data class LoginResponse(val characterName:String, val youAreAt:V3f, val orientation: V4f)

@Serializable
data class Pong(val time: Double)

@Serializable
data class Ping(val time: Double)
