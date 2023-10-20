package dev.vanadium.viml.gfx

import java.awt.Color

class RGBAColor(val red: Int, val green: Int, val blue: Int, val alpha: Int = 255) {

    init {
        require(red in 0..255)
        require(green in 0..255)
        require(blue in 0..255)
        require(alpha in 0..255)
    }

    fun awtColor(): Color {
        return Color(red, green, blue, alpha)
    }

}