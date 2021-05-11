package world.amplus.webclient

import ext.minus
import kotlinx.browser.window
import org.w3c.dom.Element
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import three.js.Camera
import three.js.Euler
import three.js.Vector2
import three.js.Vector3
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

    init {

        domElement.addEventListener("mousemove", { event ->
            if (event is MouseEvent ) {
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
