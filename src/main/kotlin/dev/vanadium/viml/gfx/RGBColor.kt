package dev.vanadium.viml.gfx

import dev.vanadium.viml.ArrayExpression
import dev.vanadium.viml.LiteralExpression
import java.awt.Color

class RGBAColor(val red: Int, val green: Int, val blue: Int, val alpha: Int = 255) {

    companion object {
        fun fromArray(arr: ArrayExpression): RGBAColor {
            if (arr.value.size !in 3..4)
                throw RuntimeException("A color is made up of an array of three to four numbers.")

            return RGBAColor(
                ((arr.value[0] as LiteralExpression<*>).value as Double).toInt(),
                ((arr.value[1] as LiteralExpression<*>).value as Double).toInt(),
                ((arr.value[2] as LiteralExpression<*>).value as Double).toInt(),
                if (arr.value.size == 4) ((arr.value[3] as LiteralExpression<*>).value as Double).toInt() else 255
            )
        }
    }

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