package dev.vanadium.wave.gfx

import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class Canvas {
    companion object {
        private var singleton: Canvas? = null

        fun instance(): Canvas {
            if (singleton == null)
                singleton = Canvas()

            return singleton!!
        }
    }

    private var image: BufferedImage? = null
    private var graphics: Graphics2D? = null

    fun initialize(width: Int, height: Int) {
        this.image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        this.graphics = this.image!!.createGraphics()
    }

    fun checkInitialized() {
        image ?: throw RuntimeException("The canvas has not been initialized.")
        graphics ?: throw RuntimeException("Graphics have not been initialized.")
    }

    fun fill(color: RGBAColor) {
        checkInitialized()
        graphics!!.color = color.awtColor()
        graphics!!.fillRect(0, 0, image!!.width, image!!.height)
    }

    fun color(color: RGBAColor) {
        checkInitialized()
        graphics!!.color = color.awtColor()
    }

    fun line(fromX: Int, fromY: Int, toX: Int, toY: Int) {
        checkInitialized()
        graphics!!.drawLine(fromX, fromY, toX, toY)
    }

    fun graphics(): Graphics2D? {
        return this.graphics
    }

    fun store(output: String) {
        checkInitialized()
        ImageIO.write(this.image, "PNG", File(output))
    }

}