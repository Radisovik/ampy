package world.amplus.webclient

import kotlin.js.Date

class Stopwatch(val name:String) {
    val startedAt = Date.now()
    fun finish(): Double {
        return Date.now() - startedAt
    }
}

object Timers {
    val watches = HashMap<String,Stopwatch>()
    fun start(name: String) {
        watches.put(name, Stopwatch(name))
    }

    fun finish(name:String): Double {
        val sw = watches.remove(name)
        if (sw !=null) {
            return sw.finish()
        }
        return Double.MIN_VALUE
    }
}
