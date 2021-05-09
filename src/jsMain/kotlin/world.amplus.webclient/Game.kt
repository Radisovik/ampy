package world.amplus.webclient

import ext.aspectRatio
import ext.minus
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.serialization.encodeToHexString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.protobuf.ProtoBuf
import stats.js.Stats
import three.js.*
import world.amplus.common.FromClient
import world.amplus.common.V3f
import kotlin.js.Date
import kotlin.js.Json

class Game {
    init {
        window.onresize = {
            camera.aspect = window.aspectRatio
            camera.updateProjectionMatrix()
            renderer.setSize(window.innerWidth, window.innerHeight)
        }
    }
    private val clock = Clock()
    private val camera = PerspectiveCamera(75, window.aspectRatio, 0.1, 1000).apply {
        position.z = 5
    }
    private val stats = Stats().apply {
        showPanel(0) // 0: fps, 1: ms, 2: mb, 3+: custom
        val root = document.getElementById("root")
        root?.appendChild(domElement)
        with (domElement.style) {
            position="fixed"
            top="0px"
            left="0px"
        }
    }


    private val renderer = WebGLRenderer().apply {
        val renderarea = window.document.getElementById("renderarea")
        renderarea?.appendChild(domElement)
        setSize(window.innerWidth, window.innerHeight)
        setPixelRatio(window.devicePixelRatio)

    }
    private val cube = Mesh(BoxGeometry(1, 1, 1), MeshPhongMaterial().apply { color = Color(0x00ffff) })

    private val scene = Scene().apply {
        add(cube)

        add(DirectionalLight(0xffffff, 1).apply { position.set(-1, 2, 4) })
        add(AmbientLight(0x404040, 1))
    }

    var positionClock = 0.toDouble()
    fun animate() {
        val now = Date.now()
        maybeSendPosition(now)

        stats.begin()
        val delta = clock.getDelta().toDouble()

        cube.rotation.x -= delta
        cube.rotation.y -= delta

        renderer.render(scene, camera)

        stats.end()

        window.requestAnimationFrame { animate() }
    }

    private fun maybeSendPosition(now: Double) {
        if (!connected) {
            return
        }
        val deltaSinceLastPositionSend = now - positionClock
        if (deltaSinceLastPositionSend > 100) {
            val iat = FromClient.iamat(
                V3f(
                    camera.position.x.toFloat(),
                    camera.position.y.toFloat(),
                    camera.position.z.toFloat()
                )
            )
            val encodeToHexString = ProtoBuf.encodeToHexString(iat)
            ws?.send(encodeToHexString)
            val ix = camera.position.x.toInt()
            val iy = camera.position.y.toInt()
            val iz = camera.position.z.toInt()
            val pmsg = "($ix,$iy,$iz)"
            document.getElementById("position")?.innerHTML = pmsg
            positionClock = now
        }
    }
}

