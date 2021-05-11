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
    var right = false
    var left = false
    var backward = false

    var mouseIsDown = false
    val _euler = Euler(0, 0, 0, "YXZ")
    val _vector = Vector3()
    val _PI_2 = kotlin.math.PI / 2;

    val minPolarAngle = 0; // radians
    val maxPolarAngle = kotlin.math.PI; // radians
    var mouse = Vector2()


    val selection = Selection(mouse, game.camera)

    init {
        domElement.addEventListener("mousemove", { event ->
            if (event is MouseEvent) {
                mouse.x = (event.clientX.toFloat() / game.renderer.domElement.clientWidth.toFloat()) * 2f - 1f
                mouse.y = -(event.clientY.toFloat() / game.renderer.domElement.clientHeight.toFloat()) * 2f + 1f


                if (mouseIsDown) {
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
            if (evt is MouseEvent) {
                mouseIsDown = true
                mdx = evt.clientX
                mdy = evt.clientY
                println("Mouse down ${evt.button}")
            }
        })
        window.document.addEventListener("keydown", { evt ->
            if (evt is KeyboardEvent) {
                val value = true
                when (evt.keyCode) {
                    87 -> forward = true
                    83 -> backward = true
                    68 -> right = true
                    65 -> left = true
                    192, 49, 50, 51, 52, 53, 54, 55, 56, 57 -> startTool(evt.keyCode)
                    else -> {
                        println("key down ${evt.code} .. ${evt.key}  ..  ${evt.keyCode}")
                    }
                }
            }
        })
        window.document.addEventListener("keyup", { evt ->
            if (evt is KeyboardEvent) {
                val value = false
                when (evt.keyCode) {
                    87 -> forward = false
                    83 -> backward = false
                    68 -> right = false
                    65 -> left = false
                    192, 49, 50, 51, 52, 53, 54, 55, 56, 57 -> endTool()
                    else -> {
                        println("key up ${evt.code} .. ${evt.key}  ..  ${evt.keyCode}")
                    }
                }
            }
        })
        domElement.addEventListener("mouseup", { evt ->
            if (evt is MouseEvent) {
                mouseIsDown = false


            }
        })
    }


    private fun endTool() {
        val sr = selection.finish()
        if (ws != null) {
            //game.chat("Sending to server")
            val tu = FromClient.tooluse(sr.startKey, sr.start, sr.end)
            val encodeToHexString = ProtoBuf.encodeToHexString(tu)
            //game.chat("Sending to server: ${JSON.stringify(tu)}")
            ws?.send(encodeToHexString)
            Timers.start("TU")
        }
    }


    private fun debug(msg: String, obj: Any?) {
        if (obj == null) {
            game.chat("$msg NULL")
        } else {
            game.chat("$msg ${JSON.stringify(obj)}")
        }
    }

    private fun startTool(keyCode: Int) {
        val additive =  (keyCode != 192)
        selection.start(additive, keyCode)
    }

    var mdx = 0
    var mdy = 0

    fun forward(distance: Float) {
        // move forward parallel to the xz-plane
        // assumes camera.up is y-up
        _vector.setFromMatrixColumn(camera.matrix, 0)
        _vector.crossVectors(camera.up, _vector)
        camera.position.addScaledVector(_vector, distance)
    }

    fun right(distance: Float) {
        _vector.setFromMatrixColumn(camera.matrix, 0)
        camera.position.addScaledVector(_vector, distance)
    }

    fun update() {
        if (!document.hasFocus()) {
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
        if (right) {
            right(.5f)
        }
        selection.update()
    }
}
