package world.amplus.server

import world.amplus.common.V3i


interface BlockStore {
    fun remove(a: V3i, b: V3i)
    fun put(a: V3i, b: V3i, bt: BlockType)
    fun put(a: V3i, bt: BlockType)
    fun remove(a: V3i)
    fun unsubscribe(chunks: ChunkName, listener: BlockStoreListener)
    fun subscribe(cn: ChunkName, version:Int, listener: BlockStoreListener)

    fun subscribe(chunks: Map<Long, Int>, listener: BlockStoreListener)
    fun get(x: Int, y: Int, z: Int): BlockType

    fun faces_for_test(): List<ExposedSide>
    fun changes_for_test(): Int
    fun facesByChunk(): Map<ChunkName, List<ExposedSide>>
    fun facesForChunks(chunks: Collection<ChunkName>): Map<ChunkName, List<ExposedSide>>
}

interface BlockStoreListener {
    fun patchChange(chunkName: ChunkName, addTheseFaces: List<Long>, textures: List<Int>, removeFaces : List<Long>, version:Int)
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


