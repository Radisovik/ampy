package world.amplus.webclient

import ext.minus
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.serialization.encodeToHexString
import kotlinx.serialization.protobuf.ProtoBuf
import org.w3c.dom.Element
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import three.js.*
import world.amplus.common.FromClient
import world.amplus.common.V3i
import kotlin.math.PI

class FirstPersonControls(val domElement: Element, val camera: Camera) {
    var forward = false
    var right =false
    var left = false
    var backward = false

    var mouseIsDown = false
    val _euler =  Euler( 0, 0, 0, "YXZ" )
    val _vector = Vector3()
    val _PI_2 = kotlin.math.PI / 2;

    val minPolarAngle = 0; // radians
    val maxPolarAngle = kotlin.math.PI; // radians

    val mousePosition = xy(0.0,0.0)



    init {
        domElement.addEventListener("mousemove", { event ->
            if (event is MouseEvent ) {
               setPickPosition(event)

                if(mouseIsDown) {
                    val movementX: Int = event.asDynamic().movementX as Int
                    val movementY: Int = event.asDynamic().movementY as Int

                    _euler.setFromQuaternion(camera.quaternion);

                    _euler.y -= movementX * 0.01;
                    _euler.x -= movementY * 0.01;
                    _euler.x = kotlin.math.max(
                        _PI_2 - maxPolarAngle,
                        kotlin.math.min(_PI_2 - minPolarAngle, _euler.x.toDouble())
                    );
                    camera.quaternion.setFromEuler(_euler);

                  //  println("Mouse moved ${event.clientX}, ${event.clientY}")
                }
            }
        })
        domElement.addEventListener("mousedown", { evt ->
            if (evt is MouseEvent ) {
                mouseIsDown = true
                mdx = evt.clientX
                mdy = evt.clientY
                println("Mouse down ${evt.button}")
            }
        })
        window.document.addEventListener("keydown", {evt ->
            if (evt is KeyboardEvent) {
                val value = true
                when(evt.keyCode) {
                    87 -> forward = true
                    83 -> backward = true
                    68 -> right = true
                    65 -> left = true
                    192, 49,50,51,52,53,54,55,56,57 -> startTool(evt.keyCode)
                    else -> {
                        println("key down ${evt.code} .. ${evt.key}  ..  ${evt.keyCode}")
                    }
                }
            }
        })
        window.document.addEventListener("keyup", {evt ->
            if (evt is KeyboardEvent) {
                val value = false
                when(evt.keyCode) {
                    87 -> forward = false
                    83 -> backward = false
                    68 -> right = false
                    65 -> left = false
                    192, 49,50,51,52,53,54,55,56,57 -> endTool(evt.keyCode)
                    else -> {
                        println("key up ${evt.code} .. ${evt.key}  ..  ${evt.keyCode}")
                    }
                }
            }
        })
//        window.document.addEventListener("keypress", {evt ->
//            println("key press $evt")
//        })
        domElement.addEventListener("mouseup", { evt ->
            if (evt is MouseEvent ) {
                mouseIsDown = false
                val dx = evt.clientX - mdx
                val dy = evt.clientY - mdy
               // camera.rotateX(2*PI *.1)
                val angle = 2*PI * (domElement.clientWidth /dx)
                camera.rotateX(angle)

                println("Mouse up ${evt.button} $dx,$dy --angle  $angle")
            }
        })
    }

    var pickPosition =xy(0.0,0.0)

    private fun setPickPosition(event: MouseEvent) {
        val pos = getCanvasRelativePosition(event)
        val canvas = window.document.getElementById("renderarea")!!
        pickPosition = xy((pos.x / canvas.clientWidth) * 2 -1, (pos.y/canvas.clientHeight) * -2 +1)
    }

    private fun getCanvasRelativePosition(event: MouseEvent): xy {
        val canvas = window.document.getElementById("renderarea")!!
            val rect = canvas.getBoundingClientRect()
            return xy((event.clientX - rect.left )* canvas.clientWidth/ rect.width,
                (event.clientY - rect.top) * canvas.clientHeight/ rect.height)

    }


    private fun endTool(keyCode: Int) {
        val toolNumber = keyCode - 49

    }

    data class xy(var x:Double, var y:Double)



    private fun startTool(keyCode: Int) {
        println("Start tool $mousePosition")
        val toolNumber = keyCode - 49
        val ray = Raycaster()
        ray.setFromCamera(pickPosition.asDynamic(), camera)
        val intersectedObjects = ray.intersectObjects(game.scene.children)
        if (intersectedObjects.isNotEmpty()) {
           // val objPos = intersectedObjects[0].`object`.position
            val position = intersectedObjects[0].point
            val face = intersectedObjects[0].face
            val normal = face!!.normal
            game.chat("Position: ${position.x}, ${position.y}, ${position.z}")
            var scaledNoraml = normal.divideScalar(2)
            if (keyCode == 192) {
                scaledNoraml = scaledNoraml.negate()
            }
            val bp = position.add(scaledNoraml)



            if (ws!=null) {
                game.chat("Sending to server")
                val tu = FromClient.tooluse(
                    keyCode,
                    V3i(bp.x.toInt(), bp.y.toInt(), bp.z.toInt()),
                    V3i(bp.x.toInt(), bp.y.toInt(), bp.z.toInt())
                )
                val encodeToHexString = ProtoBuf.encodeToHexString(tu)
                game.chat("Sending to server: ${JSON.stringify(tu)}")
                ws?.send(encodeToHexString)
            }
//            println("Position ${JSON.stringify(position)}  ${JSON.stringify(normal)} ${JSON.stringify(scaledNoraml)}")
//
//            val c = if (keyCode==192) {
//                Color(0xff0000)
//            } else {
//                Color(0x00ff00)
//            }
//            val s = Sphere(Vector3(bp.x.toInt(), bp.y.toInt(), bp.z.toInt()),1.1f)
//            val sphere = Mesh(s, MeshPhongMaterial().apply { color = c })
//            println("Adding this sphere ${JSON.stringify(sphere)}")
//            game.scene.attach(sphere)

            game.chat("I think the block is at: ${bp.x.toInt()},${bp.y.toInt()},${bp.z.toInt()} ")
        } else {
            println("Found no intersections")
        }

    }

    var mdx=0
    var mdy=0

    fun forward(distance:Float) {
        // move forward parallel to the xz-plane
        // assumes camera.up is y-up
        _vector.setFromMatrixColumn( camera.matrix, 0 )
        _vector.crossVectors( camera.up, _vector )
        camera.position.addScaledVector( _vector, distance )
    }

    fun right(distance:Float) {
        _vector.setFromMatrixColumn( camera.matrix, 0 )
        camera.position.addScaledVector( _vector, distance )
    }

    fun update() {
        if(!document.hasFocus()) {
            forward = false
            backward = false
            right = false
            left = false
            mouseIsDown = false
        }
        if (forward) {
            forward(.5f)
        }
        if (backward) {
            forward(-.5f)
        }
        if (left) {
            right(-.5f)
        }
        if(right) {
            right(.5f)
        }



    }
}
