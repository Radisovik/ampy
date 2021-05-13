package world.amplus.webclient

import ext.aspectRatio
import ext.minus
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.serialization.encodeToHexString
import kotlinx.serialization.protobuf.ProtoBuf
import stats.js.Stats
import three.js.*
import world.amplus.common.FromClient
import world.amplus.common.PlayerMoved
import world.amplus.common.V3f
import world.amplus.common.V4f
import kotlin.js.Date

class Game {
    private var inited = false
    init {
        window.onresize = {
            camera.aspect = window.aspectRatio
            camera.updateProjectionMatrix()
            renderer.setSize(window.innerWidth, window.innerHeight)
        }

    }
    private val clock = Clock()
    val camera = PerspectiveCamera(75, window.aspectRatio, 0.1, 1000).apply {
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


    val renderer = WebGLRenderer().apply {
        val renderarea = window.document.getElementById("renderarea")
        renderarea?.appendChild(domElement)
        setSize(window.innerWidth, window.innerHeight)
        setPixelRatio(window.devicePixelRatio)

    }
    private val cube = Mesh(BoxGeometry(1, 1, 1), MeshPhongMaterial().apply { color = Color(0x0000ff) })

    val terrainGroup = Group()

    val scene = Scene().apply {
        cube.position.set(2,1,2)
        attach(cube)

        val ah = AxesHelper(5)
        ah.position.set(1,1,1)
        attach(ah)

        add(DirectionalLight(0xffffff, 1).apply { position.set(-1, 2, 4) })
        add(AmbientLight(0x404040, 1))

        attach(terrainGroup)
        attach(OtherPlayer.playerGroup)
    }

    var positionClock = 0.toDouble()
    var atLeastOnce = false
    fun animate() {
        if (connected) {
            atLeastOnce = true
        }else if(!connected && atLeastOnce){
            chat("We lost connection to the server -- please click refresh")
            return
        }

        stats.begin()
        if (!inited) {
            setup()
        }
        val now = Date.now()
        maybeSendPosition(now)
        OtherPlayer.update(now)


        val delta = clock.getDelta().toDouble()

        cube.rotation.x -= delta
        cube.rotation.y -= delta

        renderer.render(scene, camera)

        controls?.update()
        stats.end()
        window.requestAnimationFrame { animate() }

       // msg("Forward: ${controls?.forward}")
    }

    var controls : FirstPersonControls? = null

    val PI = 3.1456

    val  MAX_CHAT_MESSAGES =5

   val chatMessages = Array<String>(MAX_CHAT_MESSAGES){""}

    fun chat(msg:String) {
        val chatArea = window.document.getElementById("chatarea")!!
        for (i in MAX_CHAT_MESSAGES-2 downTo 0) {
            chatMessages[i+1] = chatMessages[i]
        }
        chatMessages[0]= msg
        var chatText =""

        for (i in MAX_CHAT_MESSAGES-1 downTo 0) {
            chatText += chatMessages[i]
            chatText += "<br>"
        }
        chatArea.innerHTML = chatText
    }

    private fun setup() {
        val pg = PlaneGeometry(200, 200, 32)

        val groundTexture =TextureLoader().load("atlas.png")

        groundTexture.repeat.set(1,1)
        groundTexture.anisotropy = 16

        val groundMaterial =  MeshStandardMaterial().apply { this.map = groundTexture}

        val m = Mesh(pg, groundMaterial)
        m.position.y = -.8f
        m.rotation.x = - PI / 2
        m.receiveShadow = true
        scene.add(m)
        camera.lookAt(3,2,3)
        camera.position.set(1,4,1)
        inited = true
        controls = FirstPersonControls(renderer.domElement, camera)
    }

    var position = ""
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
                ),
                V4f(
                    camera.quaternion.x.toFloat(),
                    camera.quaternion.y.toFloat(),
                    camera.quaternion.z.toFloat(),
                    camera.quaternion.w.toFloat()
                )

            )
            val encodeToHexString = ProtoBuf.encodeToHexString(iat)
            ws?.send(encodeToHexString)
            val ix = camera.position.x.toInt()
            val iy = camera.position.y.toInt()
            val iz = camera.position.z.toInt()
            val pmsg = "($ix,$iy,$iz)"
            if (position!=pmsg) {
                document.getElementById("position")?.innerHTML = pmsg
                position = pmsg
            }
            positionClock = now
        }
    }

    fun playerMoved(pm: PlayerMoved) {
        OtherPlayer.moved(pm)

    }
}

