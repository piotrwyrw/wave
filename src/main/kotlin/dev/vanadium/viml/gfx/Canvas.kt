package dev.vanadium.viml.gfx

import java.awt.Graphics2D
import java.awt.image.BufferedImage

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

    fun graphics(): Graphics2D? {
        return this.graphics
    }

}