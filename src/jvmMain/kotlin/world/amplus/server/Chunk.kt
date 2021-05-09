package world.amplus.server

import gnu.trove.TLongIntHashMap
import world.amplus.common.Textures
import java.io.Serializable

/**
@author ehubbard on 4/25/18.
 */
data class ChunkName(val world: String, val cx: Int, val cz: Int)  {
    val name: String = "$world($cx,$cz)$CHUNK_SIZE"

    constructor() : this("NONE", Integer.MIN_VALUE, Integer.MIN_VALUE)
    constructor(world: String, wx: Float, wz: Float) : this(
        world,
        Math.floorDiv(wx.toInt(), CHUNK_SIZE),
        Math.floorDiv(wz.toInt(), CHUNK_SIZE)
    )
    constructor(world: String, packed: Long) : this(world, (packed and 0x00000000ffffffff).toInt(), (packed shr 32 and 0x00000000ffffffff).toInt() )

    fun packed() : Long {
        return (cz.toLong() shl 32) + cx.toLong()
    }

    fun myNeighbors(radius: Int, f: (ChunkName) -> Unit) {
        drawSolidCircle(cx, cz, radius) { x: Int, z: Int ->
            val ch = ChunkName(world, x, z)
            f(ch)
        }
    }

    fun myNeightbors(radius: Int): Set<ChunkName> {
        val rtn = mutableSetOf<ChunkName>()
        myNeighbors(radius) {
            rtn.add(it)
        }
        return (rtn)
    }

    override fun toString(): String {
        return name
    }

}

class Short3dArray {
    val data = Array(CHUNK_SIZE) { Array(CHUNK_SIZE) { IntArray(32) } }
    fun set(ox: Short, y: Int, oz: Short, type: Int) {
        val column = data[ox.toInt()][oz.toInt()]
        val newColumn = set(column, y, type)
        data[ox.toInt()][oz.toInt()] = newColumn // might be a no-op
    }

    fun get(ox: Short, y: Int, oz: Short): Int {
        val column = data[ox.toInt()][oz.toInt()]
        return column[y]
    }

    fun set(x: Int, y: Int, z: Int, type: Int) {
        val ox: Short = (x.and(CHUNK_SIZE_MINUS_1)).toShort()
        val oz: Short = (z.and(CHUNK_SIZE_MINUS_1)).toShort()
        set(ox, y, oz, type)
    }

    fun get(x: Int, y: Int, z: Int): Int {
        val ox: Short = (x.and(CHUNK_SIZE_MINUS_1)).toShort()
        val oz: Short = (z.and(CHUNK_SIZE_MINUS_1)).toShort()
        return get(ox, y, oz)
    }

    private fun set(array: IntArray, idx: Int, blockType: Int): IntArray {
        return if (idx >= array.size) {
            val newArray = IntArray(idx + 1)
            System.arraycopy(array, 0, newArray, 0, array.size)
            newArray[idx] = blockType
            newArray
        } else {
            array[idx] = blockType
            array
        }

    }
}

class FaceChanges(val changes : HashMap<ChunkName, ChunkFaceChanges>) {
    constructor() : this(HashMap<ChunkName, ChunkFaceChanges>())

    fun remove(chunkName: ChunkName, ek: Long, version: Int) {
        changes.getOrPut(chunkName, {ChunkFaceChanges()}).remove(ek, version)
    }

    fun add(chunkName: ChunkName, ek: Long, ordinal: Int, version: Int) {
        val faceChanges = changes.getOrPut(chunkName) { ChunkFaceChanges() }
        faceChanges.add(ek, ordinal, version)
    }
}

class ChunkFaceChanges(val addedFaces : ArrayList<Long>, val addedTextures: ArrayList<Int>, val removedFaces : ArrayList<Long>, val versions : ArrayList<Int>) {
    constructor(): this(arrayListOf<Long>(),arrayListOf<Int>(), arrayListOf<Long>(), arrayListOf<Int>())
    fun add(f: Long, t: Int, version: Int) {
        addedFaces.add(f)
        addedTextures.add(t)
        versions.add(version)
    }
    fun remove(f: Long, version: Int) {
        removedFaces.add(f)
        versions.add(version)
    }
}

data class Chunk(val chunkName: ChunkName) : Serializable {
    val data = Array(CHUNK_SIZE) { Array(CHUNK_SIZE) { IntArray(32) } }
    private val exposures = TLongIntHashMap()
    //val noEntryValue = exposures.noEntryValue
    var version = 0
    fun exposures(): TLongIntHashMap {
        return exposures
    }

    fun removeFace(changes :FaceChanges, ek: Long ): Boolean {
        // println("Remove $ek")
        if (exposures.containsKey(ek)) {
            exposures.remove(ek)
            version++
            changes.remove(chunkName, ek, version)
            return true
        }
        return false
    }

    fun addFace(changes:FaceChanges, ek: Long, texture: Textures): Int {
        val put = exposures.put(ek, texture.ordinal)
        if (put != texture.ordinal) {
            version++
            changes.add(chunkName, ek, texture.ordinal, version)

        }
        return put
    }


    fun writeJson() {


    }


//        fun generateMesh() {
//            val tids = ArrayList<Int>()
//            val sa = ArrayList<Sides>()
//            val xa = ArrayList<Int>()
//            val ya = ArrayList<Int>()
//            val za = ArrayList<Int>()
//            exposures.forEach { ek->
//                val x = ek.shr(40).and(0xFFFF).toInt()
//                val y = ek.shr(24).and(0xFFFF).toInt()
//                val z = ek.shr(8).and(0xFFFF).toInt()
//                val s = ek.and(0xFF).toInt()
//                xa.add(x)
//                ya.add(y)
//                za.add(z)
//                val side = Side.value(s.toByte())
//                sa.add(side.msgSide)
//                val byte = data[x][z][y]
//                val bt: BlockType = BlockType.btById[byte]!!
//                when(side) {
//                    Side.NX -> {
//                        tids.add(bt.left.ordinal);
//                    }
//                    Side.PX -> {
//                        tids.add(bt.right.ordinal);
//                    }
//                    Side.NY -> {
//                        tids.add(bt.bottom.ordinal);
//                    }
//                    Side.PY -> {
//                        tids.add(bt.top.ordinal);
//                    }
//                    Side.NZ -> {
//                        tids.add(bt.near.ordinal);
//                    }
//                    Side.PZ -> {
//                        tids.add(bt.far.ordinal);
//                    }
//                }
//                return@forEach true
//            }
//            val version = exposedMeshVersion+1
//            val tmb =TerrainMesh(tx, tz, xa.toIntArray(), ya.toIntArray(), za.toIntArray(), sa.toTypedArray(), tids.toIntArray(), version, "root" )
//            synchronized(this) {
//                exposedMesh = ServerMessage(tmb)
////
//
//              //  log.info("Created Terrain threeJson size of ${tmb.build().toByteArray().size}")
//
//                exposedMeshVersion++
//            }
//        //    subscribers.forEach{it.ifNotSeenSend(exposedMesh, chunkName, exposedMeshVersion)}
//        }


}
