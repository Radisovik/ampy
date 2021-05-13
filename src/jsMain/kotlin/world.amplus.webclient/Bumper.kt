package world.amplus.webclient

import three.js.Camera
import three.js.Raycaster
import three.js.Vector3

class Bumper(val direction : Vector3, val camera: Camera, range:Float) {
    val cast = Raycaster(camera.position, direction, 0, range)

    fun blocked() :Boolean {
        cast.set(camera.position, direction)
        val io = cast.intersectObjects(game.terrainGroup.children)
        if (io.isNotEmpty()) {
            val nio = io[0]
            if (nio.distance.toFloat() < 2f) {
                return true
            }
        }
        return false
    }
}

class CombinedBumper(val a:Bumper, val b:Bumper) {
    fun blocked(): Boolean {
        return a.blocked() || b.blocked()
    }
}
