package world.amplus.server

class ThrottledInvocation(private val onlyEvery:Long) {
    private var lastSaved :Long =0
    @Synchronized
    fun doStuff(stuff: () ->Unit) {
        val now = System.currentTimeMillis()
        val delta = now - lastSaved
        if (delta > onlyEvery) {
            stuff()
            lastSaved = now
        }
    }
}
