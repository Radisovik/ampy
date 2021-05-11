import java.awt.image.BufferedImage
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import javax.imageio.ImageIO

fun main() {
    try {
        val w = PrintWriter(FileWriter(File("src/commonMain/kotlin/Atlas.kt")))
        w.println("package world.amplus.common")
        w.println()
        w.println("enum class Textures(val x:Float, val y:Float, val xx:Float, val yy:Float) {")

        val imageDir = File("textures/images")

        val listFiles = imageDir.listFiles { _, name ->
            (name.toLowerCase().endsWith(".png")||(name.toLowerCase().endsWith(".jpg")))
        }
        val size = 256
        val width = size * 4
        val height = size * ((listFiles.size / 4) + 1)
        val fwidth: Float = size.toFloat() / width.toFloat()
        val fheight: Float = size.toFloat() / height.toFloat()

        val atlas = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)

        for ((ic, imgFile) in listFiles.withIndex()) {
            println("Processing: ${imgFile.name}")
            val bi = ImageIO.read(imgFile)
            val x = (ic % 4)
            val y = (ic / 4)
            atlas.graphics.drawImage(bi, x * size, y * size, null)

            val name =imgFile.name.substringBeforeLast(".") .replace("-", "_")

            val ax: Float = x * fwidth
            val ay: Float = 1 - (y * fheight)

            val bx: Float = (x + 1).toFloat() * fwidth
            val by: Float = 1 - ((y + 1).toFloat() * fheight)

            w.println("\t$name(${ax}f, ${ay}f, ${bx}f, ${by}f),")
        }
        w.println("}")
        w.close()
        ImageIO.write(atlas, "PNG", File("www/atlas.png"))

    } catch (e: Exception) {
        val file = File(".")
        e.printStackTrace()
        println("Current directory is " + file.absolutePath)
    }


}
