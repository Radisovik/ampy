package world.amplus.webclient

import three.js.*
import world.amplus.common.PlayerMoved
import kotlin.js.Date

class OtherPlayer(val name: String) {
    var lastSeen = Array<Double>(2){0.0}
    var lastsPosition = Array<Vector3>(2){Vector3()}
    val root = Object3D()

    companion object {
        fun moved(pm: PlayerMoved) {
             val op = players.getOrPut(pm.name) {
                game.chat("Hello ${pm.name}")
                OtherPlayer(pm.name)
            }
            op.moved(pm)
        }
        fun update(now:Double) {
            val justNow = now - 50
            for (player in players.values) {
                if (player.oldAndShouldBeRemoved()) {
                    playerGroup.remove(player.root)
                    players.remove(player.name)
                    game.chat("Goodbye ${player.name}")
                } else {
                    player.update(justNow)
                }

            }
        }

        val playerGroup = Group()
        val players = HashMap<String, OtherPlayer>()

        val bodyRadiusTop = .4
        val bodyRadiusBottom = .2
        val bodyHeight =2
        val bodyRadialSegments = 32
        val bodyGeometry = CylinderGeometry(bodyRadiusTop, bodyRadiusBottom, bodyHeight, bodyRadialSegments)

        val headRadius = bodyRadiusTop * 0.8
        val headLonSegments = 12
        val headLatSegments =5
        val headGeometry = SphereGeometry(headRadius, headLonSegments, headLatSegments)




       // val cg = CylinderGeometry(.5, .75, 2, 20, 20)
      //  val mat = MeshPhongMaterial().apply { color = Color(0x0000ff) }
    }
   // val ourMesh = Mesh(cg, mat)
    init {
   //     val canvas = makeLabelCanvas(10, 10, name)
      //  val texture = CanvasTexture(canvas)
       val ranGen =kotlin.random.Random(name.hashCode())
        val playerColor = Color(ranGen.nextDouble(), ranGen.nextDouble(), ranGen.nextDouble())

//        val labelMaterial = SpriteMaterial().apply {
//            map= texture
//            transparent = true
//        }

        val bodyMaterial = MeshPhongMaterial().apply {
            color=playerColor
            flatShading = true
        }



        val body = Mesh(bodyGeometry, bodyMaterial)
        root.add(body)
        body.position.y = bodyHeight /2

        val head = Mesh(headGeometry, bodyMaterial)
        root.add(head)
        head.position.y = bodyHeight + headRadius *1.1

        val labelBaseScale = .01
//        val label = Sprite(labelMaterial)
//        root.add(label)

      //  label.scale.x = canvas.width * labelBaseScale
       // label.scale.y = canvas.height *labelBaseScale

        playerGroup.attach(root)

    }

    private fun moved(pm: PlayerMoved) {
        lastSeen[1] = lastSeen[0]
        lastsPosition[1] = lastsPosition[0]
        lastSeen[0] = Date.now()
        lastsPosition[0].set(pm.position.x, pm.position.y, pm.position.z)
    }

    fun update(now : Double) {
        val seenRange = lastSeen[0] - lastSeen[1]
        val nowRange = now - lastSeen[1]
        val alpha = nowRange / seenRange
        //println("Alpha: $alpha")
        if (alpha <1 ) {
         //   println("We managed to get an alpha <1")
        }
        root.position.lerpVectors(lastsPosition[1], lastsPosition[0], alpha)
    }

    private fun oldAndShouldBeRemoved() :Boolean {
        if (Date.now() - lastSeen[0] > 10*1000) {
            return true
        }
        return false
    }

    private fun makeLabelCanvas(baseWidth:Number, size:Number, name:String) {
        val borderSize =2
        //document.createElement("canvas")?.get

    }
}
