package world.amplus.webclient

import three.js.Camera
import three.js.Raycaster
import three.js.Vector3

class Bumper(val direction: Vector3, val camera: Camera, range: Float, val cameraMod :Vector3) {
    constructor(direction: Vector3, camera: Camera, range: Float) :this(direction,camera,range, Vector3(0f,0f,0f))
    val cast = Raycaster(camera.position, direction, 0, range)

    fun blocked() :Boolean {
        val p = game.camera.position.clone()
        cast.set(p, direction)
        val io = cast.intersectObjects(game.terrainGroup.children)
        if (io.isNotEmpty()) {
          return true
        }
        return false
    }
}

class CombinedBumper(val a:Bumper, val b:Bumper) {
    fun blocked(): Boolean {
        return a.blocked() || b.blocked()
    }
}
