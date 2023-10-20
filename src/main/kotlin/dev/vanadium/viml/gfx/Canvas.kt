package dev.vanadium.viml.gfx

import java.awt.Graphics2D
import java.awt.image.BufferedImage

class Canvas {

    private var image: BufferedImage? = null
    private var graphics: Graphics2D? = null

    fun initialize(width: Int, height: Int) {
        this.image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        this.graphics = this.image!!.createGraphics()
    }

    fun graphics(): Graphics2D? {
        return this.graphics
    }

}