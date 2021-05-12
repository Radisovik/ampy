package world.amplus.webclient

import org.khronos.webgl.Float32Array
import org.khronos.webgl.set
import three.js.*
import world.amplus.common.ChunkShortName
import world.amplus.common.Side
import world.amplus.common.Side.*
import world.amplus.common.TerrainUpdates
import world.amplus.common.Textures



class ChunkData(val shortName :ChunkShortName) {
    private val exposedFaces = mutableMapOf<Long, Int>()
    private var currentMesh :Mesh<BufferGeometry, MeshStandardMaterial>? = null
    companion object {
        val atlasTexture = TextureLoader().load("atlas.png")
        val groundMaterial =  MeshStandardMaterial().apply {
            map = atlasTexture
//            this.wireframe = true
//            this.wireframeLinewidth = .25f
        }



    }

    fun process(tu: TerrainUpdates) {
        val withIndex = tu.addTheseFaces.withIndex()
        for (withIndex in withIndex) {
            exposedFaces.put(withIndex.value, tu.textures[withIndex.index])
        }
        tu.removeTheseFaces.forEach {
            exposedFaces.remove(it)
        }
        if (currentMesh!=null) {
            game.terrainGroup.remove(currentMesh!!)
        }
        if (exposedFaces.isNotEmpty()) {
            //println("Got some faces to draw: ${exposedFaces.size} ${shortName}")
            currentMesh = createMesh()
            game.terrainGroup.attach(currentMesh!!)
        }
        val delta = Timers.finish("TU")
        if (delta !=Double.MIN_VALUE) {
            game.chat("Estimated: ${delta}ms for round trip terrain change")
        }

    }

    private fun createMesh(): Mesh<BufferGeometry, MeshStandardMaterial> {
        val debugCreateMesh = false
        fun log(msg:String) {
            if (debugCreateMesh) {
                println(msg)
            }
        }
        val bg = BufferGeometry()
        log("Creating new chunk ${shortName} with this many sides: ${exposedFaces.size}")
        val wcx = shortName.cx * CHUNK_SIZE
        val wcz = shortName.cz* CHUNK_SIZE
        val wcy =0
        val positions = Float32Array(exposedFaces.size *18)
        val normals = Float32Array(exposedFaces.size * 18)
        val uv = FloatArray(exposedFaces.size * 12)
        val alreadyHasHelper = mutableSetOf<String>()
        var pi =0
        var ni=0
        var ti=0
        for (e  in exposedFaces.entries) {
            val es = ExposedSide(e.key, -1 )
            val side = Side.values()[es.side().toInt()]
                ?: throw RuntimeException("Could not determine side from: ${es.side().toByte()}  ${Side.values()}")
            val texture = Textures.values()[e.value]


            val bx = wcx + es.x()
            val by = wcy  + es.y()
            val bz = wcz +  es.z()


            val key = "$bx$by$bz"
            if (!alreadyHasHelper.contains(key) && debugCreateMesh ) {
                println("Have a new center at $bx,$by,$bz")
                val ah = AxesHelper(1)
                ah.position.set(bx, by, bz)
                game.scene.attach(ah)
                alreadyHasHelper.add(key)
            }


            when(side) {
                NX -> {


                    uv[ti++] = texture.x
                    uv[ti++] = texture.y

                    uv[ti++] = texture.xx
                    uv[ti++] = texture.y

                    uv[ti++] = texture.x
                    uv[ti++] = texture.yy

                    uv[ti++] = texture.x
                    uv[ti++] = texture.yy

                    uv[ti++] = texture.xx
                    uv[ti++] = texture.y

                    uv[ti++] = texture.xx
                    uv[ti++] = texture.yy

                    repeat(6) {
                        normals[ni++] = -1f
                        normals[ni++] = 0f
                        normals[ni++] = 0f
                    }

                    val sx = bx + side.delta.x.toFloat()/2f

                    log("Have a side at $sx,$by,$bz $side --> ${side.delta.x}")
                    positions[pi++] = sx
                    positions[pi++] = by -.5f
                    positions[pi++] = bz -.5f

                    positions[pi++] =sx
                    positions[pi++] = by -.5f
                    positions[pi++] = bz +.5f

                    positions[pi++] = sx
                    positions[pi++] = by +.5f
                    positions[pi++] = bz -.5f

                    positions[pi++] = sx
                    positions[pi++] = by +.5f
                    positions[pi++] = bz -.5f

                    positions[pi++] = sx
                    positions[pi++] = by -.5f
                    positions[pi++] = bz +.5f

                    positions[pi++] = sx
                    positions[pi++] = by +.5f
                    positions[pi++] = bz +.5f

                }
                PX -> {
                    uv[ti++] = texture.x
                    uv[ti++] = texture.y

                    uv[ti++] = texture.xx
                    uv[ti++] = texture.y

                    uv[ti++] = texture.x
                    uv[ti++] = texture.yy

                    uv[ti++] = texture.x
                    uv[ti++] = texture.yy

                    uv[ti++] = texture.xx
                    uv[ti++] = texture.y

                    uv[ti++] = texture.xx
                    uv[ti++] = texture.yy

                    repeat(6) {
                        normals[ni++] = +1f
                        normals[ni++] = 0f
                        normals[ni++] = 0f
                    }

                    val sx = bx + side.delta.x.toFloat()/2f

                    log("Have a side at $sx,$by,$bz $side --> ${side.delta.x}")

                    positions[pi++] = sx
                    positions[pi++] = by -.5f
                    positions[pi++] = bz +.5f

                    positions[pi++] = sx
                    positions[pi++] = by -.5f
                    positions[pi++] = bz -.5f

                    positions[pi++] = sx
                    positions[pi++] = by +.5f
                    positions[pi++] = bz +.5f

                    positions[pi++] = sx
                    positions[pi++] = by +.5f
                    positions[pi++] = bz +.5f

                    positions[pi++] = sx
                    positions[pi++] = by -.5f
                    positions[pi++] = bz -.5f

                    positions[pi++] = sx
                    positions[pi++] = by +.5f
                    positions[pi++] = bz -.5f

                }
                NY -> {
                    uv[ti++] = texture.x
                    uv[ti++] = texture.y

                    uv[ti++] = texture.xx
                    uv[ti++] = texture.y

                    uv[ti++] = texture.x
                    uv[ti++] = texture.yy

                    uv[ti++] = texture.x
                    uv[ti++] = texture.yy

                    uv[ti++] = texture.xx
                    uv[ti++] = texture.y

                    uv[ti++] = texture.xx
                    uv[ti++] = texture.yy

                    repeat(6) {
                        normals[ni++] = 0f
                        normals[ni++] = -1f
                        normals[ni++] = 0f
                    }

                    val sy = by + side.delta.y.toFloat()/2f

                    positions[pi++] = bx +.5f
                    positions[pi++] = sy
                    positions[pi++] = bz -.5f

                    positions[pi++] = bx -.5f
                    positions[pi++] =sy
                    positions[pi++] = bz -.5f

                    positions[pi++] = bx +.5f
                    positions[pi++] =sy
                    positions[pi++] = bz -.5f

                    positions[pi++] = bx +.5f
                    positions[pi++] = sy
                    positions[pi++] = bz -.5f

                    positions[pi++] = bx -.5f
                    positions[pi++] = sy
                    positions[pi++] = bz -.5f

                    positions[pi++] = bx -.5f
                    positions[pi++] = sy
                    positions[pi++] = bz -.5f
                }
                PY -> {
                    uv[ti++] = texture.x
                    uv[ti++] = texture.y

                    uv[ti++] = texture.xx
                    uv[ti++] = texture.y

                    uv[ti++] = texture.x
                    uv[ti++] = texture.yy

                    uv[ti++] = texture.x
                    uv[ti++] = texture.yy

                    uv[ti++] = texture.xx
                    uv[ti++] = texture.y

                    uv[ti++] = texture.xx
                    uv[ti++] = texture.yy

                    repeat(6) {
                        normals[ni++] = 0f
                        normals[ni++] = 1f
                        normals[ni++] = 0f
                    }

                    val sy = by + side.delta.y.toFloat()/2f

                    positions[pi++] = bx +.5f
                    positions[pi++] = sy
                    positions[pi++] = bz -.5f

                    positions[pi++] = bx -.5f
                    positions[pi++] = sy
                    positions[pi++] = bz -.5f

                    positions[pi++] = bx +.5f
                    positions[pi++] = sy
                    positions[pi++] = bz +.5f

                    positions[pi++] = bx +.5f
                    positions[pi++] = sy
                    positions[pi++] = bz +.5f

                    positions[pi++] = bx -.5f
                    positions[pi++] =sy
                    positions[pi++] = bz -.5f

                    positions[pi++] = bx -.5f
                    positions[pi++] = sy
                    positions[pi++] = bz +.5f

                }
                NZ -> {
                    uv[ti++] = texture.x
                    uv[ti++] = texture.y

                    uv[ti++] = texture.xx
                    uv[ti++] = texture.y

                    uv[ti++] = texture.x
                    uv[ti++] = texture.yy

                    uv[ti++] = texture.x
                    uv[ti++] = texture.yy

                    uv[ti++] = texture.xx
                    uv[ti++] = texture.y

                    uv[ti++] = texture.xx
                    uv[ti++] = texture.yy

                    repeat(6) {
                        normals[ni++] = 0f
                        normals[ni++] = 0f
                        normals[ni++] = -1f
                    }

                    val sz = bz + side.delta.z.toFloat()/2f

                    positions[pi++] = bx +.5f
                    positions[pi++] = by -.5f
                    positions[pi++] = sz

                    positions[pi++] = bx -.5f
                    positions[pi++] = by -.5f
                    positions[pi++] = sz

                    positions[pi++] = bx +.5f
                    positions[pi++] = by +.5f
                    positions[pi++] = sz

                    positions[pi++] = bx +.5f
                    positions[pi++] = by +.5f
                    positions[pi++] = sz

                    positions[pi++] = bx -.5f
                    positions[pi++] = by -.5f
                    positions[pi++] = sz

                    positions[pi++] = bx -.5f
                    positions[pi++] = by +.5f
                    positions[pi++] = sz

                }
                PZ -> {
                    uv[ti++] = texture.x
                    uv[ti++] = texture.y

                    uv[ti++] = texture.xx
                    uv[ti++] = texture.y

                    uv[ti++] = texture.x
                    uv[ti++] = texture.yy

                    uv[ti++] = texture.x
                    uv[ti++] = texture.yy

                    uv[ti++] = texture.xx
                    uv[ti++] = texture.y

                    uv[ti++] = texture.xx
                    uv[ti++] = texture.yy

                    repeat(6) {
                        normals[ni++] = 0f
                        normals[ni++] = 0f
                        normals[ni++] = +1f
                    }

                    val sz = bz + side.delta.z.toFloat()/2f

                    positions[pi++] = bx -.5f
                    positions[pi++] = by -.5f
                    positions[pi++] = sz

                    positions[pi++] = bx +.5f
                    positions[pi++] = by -.5f
                    positions[pi++] =sz

                    positions[pi++] = bx -.5f
                    positions[pi++] = by +.5f
                    positions[pi++] = sz

                    positions[pi++] = bx -.5f
                    positions[pi++] = by +.5f
                    positions[pi++] =sz

                    positions[pi++] = bx +.5f
                    positions[pi++] = by -.5f
                    positions[pi++] = sz

                    positions[pi++] = bx +.5f
                    positions[pi++] = by +.5f
                    positions[pi++] =sz
                }
            }
        }
        //game.chat("End value of PI $pi and ni $ni and ti $ti ->  ${positions.length} ${normals.length} ${uv.size}")
        bg.setAttribute("position", BufferAttribute(positions.asDynamic(),3))
        bg.setAttribute("normal", BufferAttribute(normals.asDynamic(), 3))
        bg.setAttribute("uv", BufferAttribute(uv.asDynamic(), 2))

        val m = Mesh(bg, groundMaterial)
        return m

//        bg.setIndex(arrayOf(
//            0,  1,  2,   2,  1,  3,  // front
//            4,  5,  6,   6,  5,  7,  // right
//            8,  9, 10,  10,  9, 11,  // back
//            12, 13, 14,  14, 13, 15,  // left
//            16, 17, 18,  18, 17, 19,  // top
//            20, 21, 22,  22, 21, 23,  // bottom
//        ))
    }

    private fun texture(uv: FloatArray, ti: Int, texture: Textures): Int {


        return ti
    }


}
