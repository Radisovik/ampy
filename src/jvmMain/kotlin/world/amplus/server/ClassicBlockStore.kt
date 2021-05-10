package world.amplus.server

import world.amplus.common.*
import com.hoten.delaunay.examples.TestDriver
import com.hoten.delaunay.examples.TestGraphImpl
import com.hoten.delaunay.voronoi.Center
import com.hoten.delaunay.voronoi.Corner
import org.davidmoten.hilbert.HilbertCurve
import java.awt.Color
import java.awt.Point
import java.io.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO
import kotlin.collections.HashSet
import kotlin.experimental.and
import java.util.ArrayList
import java.awt.Polygon
import java.util.logging.Logger
import kotlin.math.log2
import kotlin.streams.toList


/**
@author ehubbard on 4/14/18.
 */


data class Changes(val dirtyChunks: HashSet<ChunkName> = HashSet())

fun ek(x: Short, y: Short, z: Short, side: Side): Long {
    var rtn = 0L
    rtn += (x.and(0xffff.toShort()))
    rtn = rtn.shl(16)
    rtn += (y.and(0xffff.toShort()))
    rtn = rtn.shl(16)
    rtn += (z.and(0xffff.toShort()))
    rtn = rtn.shl(16)
    rtn += (side.ordinal.and(0xff))
    return rtn
}


fun min(a: V3i, b: V3i) = world.amplus.common.V3i(kotlin.math.min(a.x, b.x), kotlin.math.min(a.y, b.y), kotlin.math.min(a.z, b.z))

fun max(a: V3i, b: V3i) = V3i(kotlin.math.max(a.x, b.x), kotlin.math.max(a.y, b.y), kotlin.math.max(a.z, b.z))


fun ek(x: Int, y: Int, z: Int, side: Side): Long {
    val ox: Short = (x.and(CHUNK_SIZE_MINUS_1)).toShort()
    val oz: Short = (z.and(CHUNK_SIZE_MINUS_1)).toShort()
    return ek(ox, y.toShort(), oz, side)
}

const val CHUNK_SIZE: Int = 16
const val CHUNK_SIZE_MINUS_1 = CHUNK_SIZE - 1


class ClassicBlockStore(val world: String) : BlockStore {
    val logger = Logger.getLogger("ClassicBlockStore")
    val EMPTY_LONG_ARRAY = LongArray(0)
    val listeners = HashMap<ChunkName, HashSet<BlockStoreListener>>()

    @Synchronized
    override fun subscribe(cn: ChunkName, version:Int, listener: BlockStoreListener) {
            val chunk = chunk(cn)
            if (chunk.version != version) { // if they didn't have the latest
                val (longs, ints) = visit(cn)
                listener.patchChange(cn, longs.asList(), ints.asList(), EMPTY_LONG_ARRAY.asList(), chunk.version)
            }
            listeners.getOrPut(cn) { HashSet() }.add(listener)
    }



    @Synchronized
    override fun subscribe(chunks: Map<Long, Int>, listener: BlockStoreListener) {
        chunks.entries.stream()
                .forEach {
                    val cn = ChunkName("root", it.key)
                    val version = it.value
                    val chunk = chunk(cn)
                    if (chunk.version != it.value) { // if they didn't have the latest
                        val (longs, ints) = visit(cn)
                        listener.patchChange(cn, longs.asList(), ints.asList(), EMPTY_LONG_ARRAY.asList(), chunk.version)
                    }
                    listeners.getOrPut(cn) { HashSet() }.add(listener)
                }
    }

    @Synchronized
    override fun unsubscribe(it: ChunkName, listener: BlockStoreListener) {
        val get = listeners[it]
        if (get!=null) {
            get.remove(listener)
            if (get.size==0) {
                listeners.remove(it)
            }
        }else {
            logger.severe("asked to remove listener.. but it didn't exist!")
        }
    }

    fun notifyListeners(changes:FaceChanges) {
        for (entry in changes.changes.entries) {
            val cn = entry.key
            val listeners= listeners.get(cn) ?: continue
            for (listener in listeners) {
                val v = entry.value
                listener.patchChange(cn, v.addedFaces, v.addedTextures, v.removedFaces, v.versions.last() )
            }


        }
    }

    @Synchronized
    override fun remove(a: V3i) {
        val changes = FaceChanges()
        remove(changes,a.x, a.y, a.z)
        notifyListeners(changes)
    }

    @Synchronized
    override fun put(a: V3i, bt: BlockType) {
        val changes = FaceChanges()
        place(changes, a.x, a.y, a.z, bt)
        notifyListeners(changes)
    }

    @Synchronized
    override fun get(x: Int, y: Int, z: Int): BlockType {
        val chunk = chunk(x, z)
        // OFFSETS OFFSETS NOT WORLD
        val ox: Short = (x.and(CHUNK_SIZE_MINUS_1)).toShort()
        val oz: Short = (z.and(CHUNK_SIZE_MINUS_1)).toShort()
        val column = chunk.data[ox.toInt()][oz.toInt()]
        return if (y > column.size) {
            BlockType.EMPTY
        } else {
            BlockType.btById[column[y]]!!
        }
    }
//
//    override fun subscribe(worldX: Int, worldZ: Int, chunkRadius: Int, function: (ChunkName, Change) -> Unit): Subscription {
//        val sub = Subscription(worldX,worldZ,chunkRadius, function)
//        val chunkName = ChunkName("root", worldX.toFloat(), worldZ.toFloat())
//        chunkName.myNeighbors(chunkRadius) {
//            val chunk = chunks.getOrPut(it) {
//               chunk(it)
//            }
//            chunk.subscribe(sub)
//        }
//        return(sub)
//    }


    //
//    fun drawcircle(x0: Int, y0: Int, radius: Int, callback: (Int, Int) -> Unit) {
//        when (radius) {
//            1 -> callback(x0, y0)
//            else -> {
//                var x = radius - 1
//                var y = 0
//                var dx = 1
//                var dy = 1
//                var err = dx - (radius shl 1)
//
//                while (x >= y) {
//                    callback(x0 + x, y0 + y)
//                    callback(x0 + y, y0 + x)
//                    callback(x0 - y, y0 + x)
//                    callback(x0 - x, y0 + y)
//                    callback(x0 - x, y0 - y)
//                    callback(x0 - y, y0 - x)
//                    callback(x0 + y, y0 - x)
//                    callback(x0 + x, y0 - y)
//                    if (err <= 0) {
//                        y++
//                        err += dy
//                        dy += 2
//                    }
//
//                    if (err > 0) {
//                        x--
//                        dx += 2
//                        err += dx - (radius shl 1)
//                    }
//                }
//            }
//        }
//    }
    @Synchronized
    override fun changes_for_test(): Int {
        var rtn = 0
        for (chunk in chunks.values) {
            rtn += chunk.version
        }
        return rtn
    }

    @Synchronized
    fun visit(chunkName: ChunkName) :Pair<LongArray, IntArray>{
        val realChunk = chunk(chunkName)
        return realChunk.exposures().keys() to realChunk.exposures().values
    }
        @Synchronized
    override fun facesForChunks(chunks: Collection<ChunkName>): Map<ChunkName, List<ExposedSide>> {
        val rtn = mutableMapOf<ChunkName, List<ExposedSide>>()
        for (chunk in chunks) {
            val exposures = ArrayList<ExposedSide>()
            rtn[chunk] = exposures
            val realChunk = chunk(chunk)
            realChunk.exposures().forEachEntry { a, b ->
                exposures.add(ExposedSide(a, b))
            }
        }
        return rtn
    }

    @Synchronized
    override fun facesByChunk(): Map<ChunkName, List<ExposedSide>> {
        val rtn = mutableMapOf<ChunkName, List<ExposedSide>>()
        for (chunk in chunks.values) {
            val exposures = ArrayList<ExposedSide>()
            rtn.put(chunk.chunkName, exposures)
            chunk.exposures().forEachEntry { a, b ->
                exposures.add(ExposedSide(a, b))
            }
        }
        return rtn
    }

    override fun faces_for_test(): List<ExposedSide> {
        val rtn = mutableListOf<ExposedSide>()
        for (chunk in chunks.values) {
            chunk.exposures().forEachEntry { a, b ->
                rtn.add(ExposedSide(a, b))
            }
        }
        return rtn
    }

    var chunks = ConcurrentHashMap<ChunkName, Chunk>()

    fun ck(x: Int, z: Int) = x.toLong() shl 32 or (z.toLong() and 0xffffffffL)

    private fun hide(changes :FaceChanges, x: Int, y: Int, z: Int, side: Side): Boolean {
        val ch = chunk(x, z)
        val ek = ek(x, y, z, side)
        return ch.removeFace(changes, ek)
    }

    private fun hideAllMySides(changes :FaceChanges, x: Int, y: Int, z: Int) {
        val ch = chunk(x, z)
        Side.values().forEach {
            val ek = ek(x, y, z, it)
            ch.removeFace(changes, ek)
        }
    }

    private fun show(changes :FaceChanges, xx: Int, yy: Int, zz: Int, other: Side) {
        val bt = get(xx, yy, zz)
        show(changes, xx, yy, zz, other, bt)
    }

    @Synchronized
    fun show(changes:FaceChanges,x: Int, y: Int, z: Int, side: Side, type: BlockType) {
        val ch = chunk(x, z)
        val texture = type.textureBySide[side]
        ch.addFace(changes, ek(x, y, z, side), texture!!)
    }

    @Synchronized
    override fun remove(a: V3i, b: V3i) {
        val changes = FaceChanges()
        val min = min(a, b)
        val max = max(a, b)


        // iterate left
        for (z in min.z..max.z) {
            for (y in min.y..max.y) {
                remove(changes,min.x, y, z, Side.NX)
            }
        }

        // right
        for (z in min.z..max.z) {
            for (y in min.y..max.y) {
                remove(changes,max.x, y, z, Side.PX)
            }
        }

        // iterate bottom
        for (x in min.x..max.x) {
            for (z in min.z..max.z) {
                remove(changes,x, min.y, z, Side.NY)
            }
        }

        // iterate top
        for (x in min.x..max.x) {
            for (z in min.z..max.z) {
                remove(changes,x, max.y, z, Side.PY)
            }
        }

        for (x in min.x..max.x) {
            for (y in min.y..max.y) {
                remove(changes,x, y, min.z, Side.NZ)
            }
        }
        for (x in min.x..max.x) {
            for (y in min.y..max.y) {
                remove(changes,x, y, max.z, Side.PZ)
            }
        }

        // everything within the cube
        for (x in min.x + 1 until max.x) {
            for (y in min.y + 1 until max.y) {
                for (z in min.z + 1 until max.z) {
                    remove(changes,x, y, z)
                }
            }
        }
        notifyListeners(changes)
    }


    @Synchronized
    override fun put( a: V3i, b: V3i, bt: BlockType, ) {
        if (bt == BlockType.EMPTY) throw RuntimeException("Can not place empty blocks!")
        val changes = FaceChanges()
        val min = min(a, b)
        val max = max(a, b)

        // iterate left
        for (z in min.z..max.z) {
            for (y in min.y..max.y) {
                place(changes, min.x, y, z, bt, Side.NX)
            }
        }

        // right
        for (z in min.z..max.z) {
            for (y in min.y..max.y) {
                place(changes, max.x, y, z, bt, Side.PX)
            }
        }

        // iterate bottom
        for (x in min.x..max.x) {
            for (z in min.z..max.z) {
                place(changes, x, min.y, z, bt, Side.NY)
            }
        }

        // iterate top
        for (x in min.x..max.x) {
            for (z in min.z..max.z) {
                place(changes, x, max.y, z, bt, Side.PY)
            }
        }

        for (x in min.x..max.x) {
            for (y in min.y..max.y) {
                place(changes, x, y, min.z, bt, Side.NZ)
            }
        }
        for (x in min.x..max.x) {
            for (y in min.y..max.y) {
                place(changes, x, y, max.z, bt, Side.PZ)
            }
        }

        // everything within the cube
        for (x in min.x + 1 until max.x) {
            for (y in min.y + 1 until max.y) {
                for (z in min.z + 1 until max.z) {
                    block(x, y, z, bt)
                    hideAllMySides(changes, x, y, z)
                }
            }
        }
        notifyListeners(changes)

    }

    private fun empty(x: Int, y: Int, z: Int): Boolean {
        if (y < 0) return true
        val chunk = chunk(x, z)
        // OFFSETS OFFSETS NOT WORLD
        val ox: Short = (x.and(CHUNK_SIZE_MINUS_1)).toShort()
        val oz: Short = (z.and(CHUNK_SIZE_MINUS_1)).toShort()
        val column = chunk.data[ox.toInt()][oz.toInt()]
        return !(y < column.size && column[y] != BlockType.EMPTY.ordinal)
    }

    private fun place( changes :FaceChanges, x: Int, y: Int, z: Int, type: BlockType, vararg onlySide: Side) {
        block(x, y, z, type)

        val onlyThese = if (onlySide.isEmpty()) {
            Side.values()
        } else {
            onlySide
        }

        for (side in onlyThese) {
            val d = side.delta
            val xx = x + d.x
            val yy = y + d.y
            val zz = z + d.z

            if (!hide(changes, xx, yy, zz, side.other())) {
                show(changes, x, y, z, side, type )
            }
        }
    }

    private fun remove(changes:FaceChanges,x: Int, y: Int, z: Int) {
        for (s in Side.values()) {
            remove(changes,x, y, z, s)
        }
    }

    private fun remove(changes:FaceChanges,x: Int, y: Int, z: Int, side: Side) {
        val chunk = chunk(x, z)
        // OFFSETS OFFSETS NOT WORLD
        val ox: Short = (x.and(CHUNK_SIZE_MINUS_1)).toShort()
        val oz: Short = (z.and(CHUNK_SIZE_MINUS_1)).toShort()
        val column = chunk.data[ox.toInt()][oz.toInt()]
        column[y] = BlockType.EMPTY.ordinal

        val d = side.delta
        val xx = x + d.x
        val yy = y + d.y
        val zz = z + d.z

        if (!empty(xx, yy, zz))
            show(changes, xx, yy, zz, side.other())

        hide(changes, x, y, z, side)

    }


    private fun block(x: Int, y: Int, z: Int, type: BlockType) {
        val chunk = chunk(x, z)
        // OFFSETS OFFSETS NOT WORLD
        val ox: Short = (x.and(CHUNK_SIZE_MINUS_1)).toShort()
        val oz: Short = (z.and(CHUNK_SIZE_MINUS_1)).toShort()
        val column = chunk.data[ox.toInt()][oz.toInt()]
        val newColumn = set(column, y, type.ordinal)
        chunk.data[ox.toInt()][oz.toInt()] = newColumn // might be a no-op
    }


    private fun set(array: IntArray, idx: Int, blockType: Int): IntArray {
        return if (idx >= array.size) {
            val newArray = IntArray(idx + 1)
            System.arraycopy(array, 0, newArray, 0, array.size)
            newArray[idx] = blockType
            newArray
        } else {
            array[idx] = blockType
            array
        }

    }
//
//    fun loadMap() {
//        val f = File("map.json.gz")
//        if (f.exists()) {
//            log.info("Loading map from disk...[START]")
//
//            val jr = JsonReader(InputStreamReader(InflaterInputStream(FileInputStream(f))))
//            val g = Gson()
//            chunks = g.fromJson(jr, ConcurrentHashMap::class.java)
//
//            log.info("Loading map from disk...[DONE]")
//        } else {
//            generateMap()
//        }
//    }
//
//    fun saveMap() {
//        log.info("Saving map...[START]")
//        val f = File("map.json.gz")
//        val g = Gson()
//
//        val de = PrintWriter(DeflaterOutputStream(FileOutputStream(f)))
//        de.write(g.toJson(chunks))
//        de.close()
//        log.info("Saving map...[DONE]")
//
//    }


    fun generateMap() {
        put(V3i(5, 5, 10), BlockType.DIRT)


        val random = Random(1)
//        log.info("Generating map [START]")
//        put( V3i(-512, 1,  -512), V3i(512,3,512), BlockType.DIRT)
//        put( V3i(-512, 4,  -512), V3i(512,4,512), BlockType.DIRT_GRASS)
//        val area = (1024*1024).toFloat()
//        // 5% blocks
//        log.info("Plopping some random stone")
//        repeat ( (area * .001).toInt()) {
//            val x = random.nextInt(1024)-512
//            val z = random.nextInt(1024)-512
//            put(V3i(x, 5, z), BlockType.STONE)
//        }
//        log.info("Plopping some random granite")
//        // 5% blocks
//        repeat ( (area * .05).toInt()) {
//            val x = random.nextInt(1024)-512
//            val z = random.nextInt(1024)-512
//            put(V3i(x, 5, z), BlockType.GRANITE)
//        }
//        log.info("Plopping some random ice")
//        // 5% blocks
//        repeat ( (area * .05).toInt()) {
//            val x = random.nextInt(1024)-512
//            val z = random.nextInt(1024)-512
//            put(V3i(x, 5, z), BlockType.ICE)
//        }
        log.info("Generating map [DONE]")
        chunks.values.forEach {
            it.writeJson()
        }
        // saveMap()
    }


    fun loadMap2() {
        val changes = FaceChanges()
        log.info("Loading map")
        val bi = ImageIO.read(File("server/resources/map.png"))
        log.info("Placing blocks")
        val start = System.currentTimeMillis()
        for (x in 0 until bi.getWidth(null)) {
            log.info("$x")
            for (z in 0 until bi.getHeight(null)) {
                val rgb = bi.getRGB(x, z)
                val c = Color(rgb)
                when (rgb) {
                    -1 -> {
                        put(V3i(x, 1, z), V3i(x, 3, z), BlockType.DIRT)
                        put(V3i(x, 4, z), V3i(x, 5, z), BlockType.ICE)
                    }
                    -12303238 -> {
                        put(V3i(x, 1, z), BlockType.WATER)
                    }
                    -6254473 -> {  //beach
                        put(V3i(x, 1, z), V3i(x, 3, z), BlockType.DIRT)
                    }
                    -11167420 -> {
                        put(V3i(x, 1, z), V3i(x, 3, z), BlockType.DIRT)
                        put(V3i(x, 4, z), BlockType.GRASS)
                    }
                    else -> {
                        println()
                    }
                }

            }
        }
        log.info("Done! ${(System.currentTimeMillis() - start) / 1000}seconds")
    }

    private fun chunk(x: Int, z: Int): Chunk {
        val cx = Math.floorDiv(x, CHUNK_SIZE)
        val cz = Math.floorDiv(z, CHUNK_SIZE)
        val key = ChunkName("root", cx, cz)
        return chunk(key)
    }

    private fun chunk(k: ChunkName): Chunk {
        return chunks.getOrPut(k) {
            val ch = Chunk(k)
            ch
        }
    }
//
//    @Synchronized
//    fun add(p: Internal.Paint, save: Boolean = true) {
//
//        changes.set(Changes())
//        val paints = splitupPaint(p)
//        for (pe in paints) {
//            val paint = pe.value
//            val chunkName = pe.key
//            if (paint.blockType == BlockType.EMPTY) {
//                if (paint.b == null) {
//                    remove(paint.a!!)
//                } else {
//                    remove(paint.a!!, paint.b!!)
//                }
//            } else {
//                if (paint.b == null) {
//                    put(paint.a!!, paint.blockType!!)
//                } else {
//                    put(paint.a!!, paint.b!!, paint.blockType!!)
//                }
//            }
//            if (save) {
//                saver.savePaint(world, chunkName.toString(), paint)
//            }
//        }
//        val chgs = changes.get()
//        val faces = createFaces(chgs.dirtyChunks)
//        for (face in faces) {
//            val listeners = listeners.get(face.key)
//            listeners.forEach {
//                var sendThis: String? = null
//                if (sendThis == null) {
//                    sendThis = it.convert(face.key, face.value)
//                }
//                it.notify(face.key, sendThis)
//            }
//        }
//
//        changes.remove()
//    }
//
//    val listeners = HashMap<ChunkName, HashSet<ChunkListener>>()
//
//    fun listenTo(addThese: Set<ChunkName>, listener: ChunkListener) {
//        addThese.forEach {
//            listeners.put(it, listener)
//        }
//        val firstSet = createFaces(addThese)
//        for (entry in firstSet) {
//            val string = listener.convert(entry.key, entry.value)
//            listener.notify(entry.key, string)
//        }
//    }
//
//    fun stopListening(removeThese: Set<ChunkName>, listener: ChunkListener) {
//        removeThese.forEach {
//            listeners.remove(it, listener)
//        }
//    }

    private fun createFaces(chunks: Set<ChunkName> = facesByChunk().keys): Map<ChunkName, Map<Int, Map<Int, ArrayList<Byte>>>> {
        val rtn = mutableMapOf<ChunkName, Map<Int, Map<Int, ArrayList<Byte>>>>()
        for (chunkKey in chunks) {
            val data = mutableMapOf<Int, MutableMap<Int, ArrayList<Byte>>>()
            facesByChunk().get(chunkKey)?.forEach {
                val t2coord = data.getOrPut(it.side().toInt()) { mutableMapOf() }
                val spotForCoords = t2coord.computeIfAbsent(it.tid) { ArrayList() }
                val coords =
                    listOf(it.x().toByte(), it.y().toByte(), it.z().toByte())
                spotForCoords.addAll(coords)
            }
            rtn.put(chunkKey, data)
//            val ox = chunkKey.cx * CHUNK_SIZE
//            val oz = chunkKey.cz * CHUNK_SIZE
//            val chunkFaces = MessageFromServer.chunkFaces(chunkKey.name, ox, oz, data)
//            rtn.put(chunkKey, chunkFaces)
        }
        return rtn
    }

//    val graph = TestDriver.createVoronoiGraph(1024, 16000, 1, 128, "perlin")
//    val hc = HilbertCurve.small().bits(log2(graph.bounds.width * 2).toInt()).dimensions(2)
//    val sortedCorners by lazy {
//        ImageIO.write(graph.createMap(), "PNG", File("current.png"))
//        val centers = TreeMap<Long, Corner>()
//        graph.corners.forEach {
//            val idx = hc.index(it.loc.x.toLong(), it.loc.y.toLong())
//            centers.put(idx, it)
//        }
//        centers
//    }

//    fun log2(x: Int): Int {
//        return (Math.log(x.toDouble()) / Math.log(2.0)).toInt()
//    }

    fun min(x: Long, y: Int): Long {
        return kotlin.math.min(x, y.toLong())
    }

    fun max(x: Long, y: Int): Long {
        return kotlin.math.max(x, y.toLong())
    }

    val generatedChunks = mutableSetOf<ChunkName>()

//    fun ensureGenerated(addThese: Set<ChunkName>) {
//        val changes = FaceChanges()
//        fun add(polygon: Polygon, loc: com.hoten.delaunay.geom.Point) {
//            polygon.addPoint(loc.x.toInt(), loc.y.toInt())
//        }
//
//        val halfWidth = graph.bounds.width / 2
//        addThese.forEach {
//            val sx = ((16 * it.cx) + halfWidth).toLong()
//            val sy = ((16 * it.cz) + halfWidth).toLong()
//            val ex = (sx + 16).toLong()
//            val ey = (sy + 16).toLong()
//            val queryRanges = hc.query(longArrayOf(sx, sy), longArrayOf(ex, ey), 10)
//            val centersDone = mutableSetOf<Center>()
//            queryRanges.forEach { it ->
//                sortedCorners.subMap(it.low(), it.high()).values.forEach { corner ->
//                    corner.touches.forEach { center ->
//                        if (!centersDone.contains(center)) {
//                            val points = center.corners.stream().map { it.loc }.toList()
//                            val sortedPoints = sortVerticies(points)
//                            val p = Polygon()
//                            sortedPoints.forEach { sp ->
//                                add(p, sp)
//                            }
//                            for (x in sx..ex) {
//                                for (y in sy..ey) {
//                                    if (p.contains(x.toInt(), y.toInt())) {
//                                        var total = 0.toDouble()
//                                        var count = 0.toDouble()
//
//                                        center.corners.forEach { corner1 ->
//                                            val ds = distanceSquared(
//                                                x.toInt(),
//                                                y.toInt(),
//                                                corner1.loc.x.toInt(),
//                                                corner1.loc.y.toInt()
//                                            )
//                                            val inds = 1f / ds.toFloat()
//                                            count += inds
//                                            total += inds * corner1.elevation
//                                            this.put(
//                                                V3i(corner1.loc.x.toInt(), 0, corner1.loc.y.toInt()),
//                                                V3i(
//                                                    corner1.loc.x.toInt(),
//                                                    corner1.elevation.toInt() * 256,
//                                                    corner1.loc.y.toInt()
//                                                ),
//                                                BlockType.RED_BRICK
//                                            )
//                                        }
//                                        val e = total / count
//                                        val fh = e * 256
//                                        val mx = (x - halfWidth).toInt()
//                                        val my = (y - halfWidth).toInt()
//
//                                        val bt = when (center.biome) {
//                                            TestGraphImpl.ColorData.OCEAN -> BlockType.WATER
//                                            TestGraphImpl.ColorData.LAKE -> BlockType.WATER
//                                            TestGraphImpl.ColorData.SNOW -> BlockType.SNOW
//                                            TestGraphImpl.ColorData.GRASSLAND -> BlockType.GRASS
//                                            TestGraphImpl.ColorData.ICE -> BlockType.ICE
//                                            TestGraphImpl.ColorData.RIVER -> BlockType.WATER
//                                            TestGraphImpl.ColorData.MARSH -> BlockType.PEBBLES
//
//                                            else -> BlockType.RED_BRICK
//                                        }
//                                        this.put(V3i(mx, 0, my), V3i(mx, fh.toInt(), my), bt)
//                                    }
//                                }
//                            }
//
//
//
//
//
//
//                            centersDone.add(center)
//                        }
//                    }
//                }
//            }
//
//        }
//
//
//
//        fun distanceSquared(x: Long, y: Long, xx: Int, yy: Int): Int {
//            val dx = x - xx
//            val dy = y - yy
//            return (dx * dx + dy * dy).toInt()
//        }
//
//
//    }



    interface ChunkListener {
        fun notify(chunkName: ChunkName, mfs: String)
        fun convert(chunkName: ChunkName, data: Map<Int, Map<Int, ArrayList<Byte>>>): String

    }

    object log {

        fun info(msg: String) {
            println(msg)
        }
    }

}


fun sortVerticies(points: List<com.hoten.delaunay.geom.Point>): List<com.hoten.delaunay.geom.Point> {
    // get centroid
    val center = findCentroid(points)
    Collections.sort(points) { a, b ->
        val a1 = (Math.toDegrees(Math.atan2((a.x - center.x).toDouble(), (a.y - center.y).toDouble())) + 360) % 360
        val a2 = (Math.toDegrees(Math.atan2((b.x - center.x).toDouble(), (b.y - center.y).toDouble())) + 360) % 360
        (a1 - a2).toInt()
    }
    return points
}

fun findCentroid(points: Collection<com.hoten.delaunay.geom.Point>): java.awt.Point {
    var x = 0.toDouble()
    var y = 0.toDouble()
    points.forEach {
        x += it.x
        y += it.y
    }
    val center = Point((x / points.size).toInt(), (y / points.size).toInt())
    return center
}
