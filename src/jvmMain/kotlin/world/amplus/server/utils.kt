package world.amplus.server

import java.io.File
import java.util.*
import java.nio.ByteOrder






fun getBuildNumber(): Int {
    val p = BlockType.javaClass.classLoader.getResourceAsStream("build.properties")
        ?: throw RuntimeException("Failed to load build.properties")
    val prop = Properties()
    prop.load(p)
    val sbuildnumber = prop.getProperty("build.number")
    val number = Integer.parseInt(sbuildnumber)
    return number
}


fun drawSolidCircle(x: Int, y: Int, radius: Int, f: (x: Int, y: Int) -> Unit) {
    if (radius == 0) {
        f(x, y)
        return
    }
    if (radius == 1) {
        f(x, y)
        f(x - 1, y)
        f(x + 1, y)
        f(x, y - 1)
        f(x, y + 1)
        return
    }

    val points = ArrayList<Pair<Int, Int>>()
    val rr = radius * radius
    for (xx in x - radius..x + radius) {
        for (yy in y - radius..y + radius) {
            if (distanceSquared(x, y, xx, yy) <= rr) {
                points.add(Pair(xx, yy))
            }
        }
    }

    points.sortBy { distanceSquared(x, y, it.first, it.second) }
    points.forEach { f(it.first, it.second) }


//    f(x, y)
//
//    if (radius == 0) return
//
//
//    val rr = radius * radius
//
//
//    for (r in 1..radius) {
//        top(x, r, y, rr, f)
//        bottom(x, r, y, rr, f)
//        left(x, r, y, rr, f)
//        right(x, r, y, rr, f)
//
//    }


}

private fun top(x: Int, radius: Int, y: Int, rr: Int, f: (x: Int, y: Int) -> Unit) {
    val a = x - radius
    val b = x + radius
    val yy = y - radius
    for (xx in a..b) {
        if (distanceSquared(x, y, xx, yy) <= rr) {
            f(xx, yy)
        }
    }
}


private fun bottom(x: Int, radius: Int, y: Int, rr: Int, f: (x: Int, y: Int) -> Unit) {
    val a = x - radius
    val b = x + radius
    val yy = y + radius
    for (xx in a..b) {
        if (distanceSquared(x, y, xx, yy) <= rr) {
            f(xx, yy)
        }
    }
}


private fun left(x: Int, radius: Int, y: Int, rr: Int, f: (x: Int, y: Int) -> Unit) {
    val a = y - radius
    val b = y + radius
    val xx = x - radius
    for (yy in a..b) {
        if (distanceSquared(x, y, xx, yy) <= rr) {
            f(xx, yy)
        }
    }
}


private fun right(x: Int, radius: Int, y: Int, rr: Int, f: (x: Int, y: Int) -> Unit) {
    val a = y - radius
    val b = y + radius
    val xx = x + radius
    for (yy in a..b) {
        if (distanceSquared(x, y, xx, yy) <= rr) {
            f(xx, yy)
        }
    }
}




fun distanceSquared(x: Int, y: Int, xx: Int, yy: Int): Int {
    val dx = x - xx
    val dy = y - yy
    return dx * dx + dy * dy
}
