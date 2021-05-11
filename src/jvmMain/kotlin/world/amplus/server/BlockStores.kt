package world.amplus.server

import world.amplus.common.V3i
import kotlin.random.Random

object BlockStores {
    private val store = ClassicBlockStore("root")
    val r = Random(0)
    init {
        val bs = store

        bs.put(V3i(-124, 0, -124), V3i(124, 0,124), BlockType.DIRT)
        bs.put(V3i(-23, 1, 23), V3i(23, 1,23), BlockType.GRASS)
        bs.put(V3i(23, 1, 23), V3i(23, 1,-23), BlockType.GRASS)
        bs.put(V3i(23, 1, -23), V3i(-23, 1,-23), BlockType.GRASS)
        bs.put(V3i(-23, 1, -23), V3i(-23, 1,23), BlockType.GRASS)
        bs.put(V3i(1,1,1), BlockType.EXPLOSIVES_LIT)

        repeat(100) {
            val rbt = BlockType.values()[r.nextInt(BlockType.values().size)]
            store.put(rv3i(), rbt)
        }
    }

    fun rv3i(): V3i {
        return V3i(r.nextInt(-127, 127),2,r.nextInt(-127, 127))
    }

    fun blockStore(worldName:String) :BlockStore {
        return store
    }

}
