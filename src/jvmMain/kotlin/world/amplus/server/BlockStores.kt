package world.amplus.server

import world.amplus.common.V3i

object BlockStores {
    private val store = ClassicBlockStore("root")
    init {
        store.put(V3i(5,5,5), BlockType.DIRT)
    }

    fun blockStore(worldName:String) :BlockStore {
        return store
    }

}
