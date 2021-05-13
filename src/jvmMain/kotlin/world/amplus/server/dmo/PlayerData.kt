package world.amplus.server.dmo

import com.google.gson.Gson
import com.mongodb.BasicDBObject
import com.mongodb.MongoClient
import com.mongodb.client.model.Filters
import io.ktor.util.*
import org.apache.commons.codec.binary.Hex
import org.eclipse.collections.api.set.primitive.CharSet
import org.mapdb.DBMaker
import org.mapdb.HTreeMap
import world.amplus.common.V3f
import world.amplus.common.V4f
import world.amplus.server.ConnectedClient
import world.amplus.server.ThrottledInvocation
import world.amplus.server.ds.MyCoolMap
import world.amplus.server.logger
import java.io.File
import java.io.FileReader
import java.io.FilenameFilter
import java.nio.charset.Charset
import java.nio.file.Files
import java.util.concurrent.ConcurrentMap
import java.util.logging.Logger

data class PlayerData(val playerName:String="",  val uid:String, val email:String, val location: V3f, val orientation: V4f) {
    constructor(uid:String) : this("", uid, uid, V3f(1f, 1f,1f), V4f(1f,1f,1f,1f))

    companion object {
        val gson = Gson()
        val logger = Logger.getLogger("PlayerDB")
        val saveDir = File("data/players")
        val playerByUID = HashMap<String, PlayerData>()
        val playerByEmail = HashMap<String, PlayerData>()
        val playerByName = HashMap<String, PlayerData>()
        val dirtyPlayers = HashSet<String>()
        var lastSave =System.currentTimeMillis()

        init {
            if (!saveDir.exists()) {
                logger.info("Save dir for players doesn't exist.. creating it")
                saveDir.mkdirs()
            } else {
                for (file in saveDir.listFiles()) {
                    if (file.name.endsWith(".json")) {
                        val pd = gson.fromJson(FileReader(file), PlayerData::class.java)
                        playerByUID.put(pd.uid, pd)
                        playerByEmail.put(pd.email, pd)
                        playerByName.put(pd.playerName, pd)
                    }
                }
                logger.info("Player data for ${playerByUID.size} loaded")
            }
        }
        val random = java.util.Random()
        fun nextUid() :String {
            return "${random.nextInt()}"
        }

        fun byEmail(email : String): PlayerData? {
            return playerByEmail.get(email)
        }

        fun byName(name: String): PlayerData? {
            return playerByName.get(name)
        }

        fun byUid(uid :String): PlayerData? {
            return playerByUID.get(uid)
        }

        @Synchronized
        fun savePlayer(pd:PlayerData) {
            playerByUID.put(pd.uid, pd)
            playerByEmail.put(pd.email, pd)
            playerByName.put(pd.playerName, pd)
            dirtyPlayers.add(pd.uid)
            val now = System.currentTimeMillis()
            val delta = now - lastSave
            if (delta > 10*1000) {
                // the save is in-line here..  inside this single synchronized because
                // I don't want others to call it on accident
                //  future work ehre to make the maps fully persistent/functional
                    // so the save operation can take place on a snapshot of the maps
                logger.info("Saving player data to disk")
                val start = System.currentTimeMillis()
                for (dirtyPlayer in dirtyPlayers) {
                    val dp = playerByUID[dirtyPlayer]!!
                    val hex = hex(pd.uid.toByteArray())
                    val f = File(saveDir, "${hex}.json")
                    val pjson = gson.toJson(dp)
                    f.writeBytes(pjson.toByteArray())
                }
                val delta = System.currentTimeMillis() - start
                logger.info("Flushed ${dirtyPlayers.size} players took ${delta}ms")
                lastSave = now
            }
        }
    }

    fun updatePosition(p : V3f, o: V4f) {
        val npd = this.copy(location = p, orientation = o)
        savePlayer(npd)
    }

}





