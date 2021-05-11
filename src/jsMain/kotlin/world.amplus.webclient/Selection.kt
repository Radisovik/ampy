package world.amplus.webclient

import three.js.*
import world.amplus.common.V3i

class Selection(val mouse:Vector2, val camera:Camera) {
    var startAt :V3i? = null
    var endAt :V3i? = null
    var additive = true
    var dragging = false
    var kc :Int=0
    val selectionRay = Raycaster(far = 32)
    val selectionGroup = Group()

    val addBox = Mesh( BoxGeometry(1.1, 1.1, 1.1), MeshPhongMaterial().apply {
        color = Color(0x00ff00)
        opacity = 0.25f
        transparent = true
    })

    val removeBox = Mesh( BoxGeometry(1.1, 1.1, 1.1), MeshPhongMaterial().apply {
        color = Color(0xff0000)
        opacity = 0.25f
        transparent = true
    })

    var dragBox = Mesh( BoxGeometry(1.1, 1.1, 1.1), MeshPhongMaterial().apply {
        color = Color(0x0000ff)
        opacity = 0.5f
        transparent = true
    })

    init {
        game.scene.attach(selectionGroup)
        selectionGroup.attach(addBox)
        selectionGroup.attach(removeBox)
    }

    fun start(add:Boolean, keyCode:Int) :Boolean{
        if (dragging) return false
        startAt  = pointedAt(add) ?: return false

        selectionGroup.remove(addBox)
        selectionGroup.remove(removeBox)
        selectionGroup.attach(dragBox)

        endAt = startAt
        dragging = true
        additive = add
        game.scene.attach(addBox)
        kc = keyCode
       // game.chat("Start selection at ${JSON.stringify(startAt)}")
        return true
    }

    fun update()  {
            if(!dragging) {
                val ifAdding = pointedAt(true)
                if (ifAdding!=null) {
                    addBox.position.set(ifAdding.x, ifAdding.y, ifAdding.z)
                }
                val ifRemoving = pointedAt(false)
                if (ifRemoving!=null) {
                    removeBox.position.set(ifRemoving.x, ifRemoving.y, ifRemoving.z)
                }


            } else {
                val pa = pointedAt(additive)
                if (pa!=null) {
                    endAt = pa
                    val mp = mid(startAt!!, endAt!!)

                    val sx = kotlin.math.abs(startAt!!.x - endAt!!.x)+1
                    val sy = kotlin.math.abs(startAt!!.y - endAt!!.y)+1
                    val sz = kotlin.math.abs(startAt!!.z - endAt!!.z)+1
                    val nb = newBox(sx, sy, sz, mp.x, mp.y, mp.z)
                    selectionGroup.remove(dragBox)
                    selectionGroup.attach(nb)
                    dragBox = nb
                }
            }
        }

    private fun newBox(width:Number, height:Number, depth:Number, x:Number, y:Number, z:Number): Mesh<BoxGeometry, MeshPhongMaterial> {
        val m =  Mesh( BoxGeometry(width, height, depth), MeshPhongMaterial().apply {
            color = Color(0x0000ff)
            opacity = 0.5f
            transparent = true
        })
        m.position.set(x,y,z)
        return m
    }

    fun finish() :SelectionResult {

        selectionGroup.attach(addBox)
        selectionGroup.attach(removeBox)
        selectionGroup.remove(dragBox)


        val sx = kotlin.math.abs(startAt!!.x - endAt!!.x)+1
        val sy = kotlin.math.abs(startAt!!.y - endAt!!.y)+1
        val sz = kotlin.math.abs(startAt!!.z - endAt!!.z)+1

        val rtn = SelectionResult(startAt!!, endAt!!,kc, sx*sy*sz)
        game.chat("Selection done: $rtn")
        dragging= false
        return rtn
    }

    private fun mid(a :V3i, b:V3i) :Vector3 {
        return Vector3(mid(a.x, b.x), mid(a.y, b.y), mid(a.z, b.z))
    }

    private fun mid(x :Int,xx:Int) :Double {
        return (x.toDouble() + xx.toDouble()) /2.toDouble()
    }

    private fun pointedAt(adding:Boolean): V3i? {
        selectionRay.setFromCamera(mouse, camera)
        val intersects = selectionRay.intersectObjects(game.terrainGroup.children)
        if (intersects.isNotEmpty()) {
            val i = intersects[0]
            val f = i.face!!
            return deduceBlock(adding, i.point, f.normal)
        } else {
            return null
        }
    }






    fun deduceBlock(adding: Boolean, point:Vector3, normal:Vector3) :V3i{
        var ah =if(adding) {
            point.add(normal.divideScalar(2))
        } else {
            point.sub(normal.divideScalar(2))
        }
        ah = ah.round()
        return V3i(ah.x.toInt(), ah.y.toInt(), ah.z.toInt())
    }

    data class SelectionResult(val start :V3i, val end:V3i, val startKey:Int, val size:Int)

}
