package world.amplus.server

import world.amplus.common.Side
import world.amplus.common.Textures


enum class BlockType(
    val top: Textures,
    val bottom: Textures,
    val left: Textures,
    val right: Textures,
    val near: Textures,
    val far: Textures
) {
    // EMPTY MUST BE FIRST SO that 0, which is default, is what our block store
    // initializes to
    EMPTY(),
    GRASS(Textures.grass, Textures.dirt, Textures.dirt_side_grass),
    WATER(Textures.water),
    DIRT(Textures.dirt),
    GRANITE(Textures.granite),
    RED_BRICK(Textures.redbrick),
    SNOW(Textures.snow, Textures.dirt, Textures.dirt_side_snow),
    PEBBLES(Textures.pebbles),
    ICE(Textures.ice),
    EXPLOSIVES(Textures.tntunlit, Textures.boxcrate, Textures.boxcrate),
    EXPLOSIVES_LIT(Textures.littnt, Textures.boxcrate, Textures.boxcrate),
    BED(Textures.bedtop, Textures.bedside,Textures.bedside),
    MOSS_ROCK(Textures.moss, Textures.rock, Textures.rocksidewithmoss),
    ROCK(Textures.rock);

    companion object {
        val btById = mutableMapOf<Int, BlockType>()

        init {
            values().forEach {
                btById[it.ordinal] = it
            }
        }

    }

    val textureBySide = mutableMapOf<Side, Textures>()

    init {
        textureBySide[Side.NX] = left
        textureBySide[Side.PX] = right
        textureBySide[Side.NY] = bottom
        textureBySide[Side.PY] = top
        textureBySide[Side.NZ] = near
        textureBySide[Side.PZ] = far
    }

    constructor(t: Textures) : this(t, t, t, t, t, t)
    constructor(top: Textures, bot: Textures, sides: Textures) : this(top, bot, sides, sides, sides, sides)
    constructor() : this(Textures.dirt)

}


