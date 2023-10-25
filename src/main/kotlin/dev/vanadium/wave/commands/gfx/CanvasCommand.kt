package dev.vanadium.wave.commands.gfx

import dev.vanadium.wave.ArrayExpression
import dev.vanadium.wave.DOUBLE_TYPE
import dev.vanadium.wave.ExpressionNode
import dev.vanadium.wave.LiteralExpression
import dev.vanadium.wave.gfx.Canvas
import dev.vanadium.wave.gfx.RGBAColor
import dev.vanadium.wave.handler.ArgumentType
import dev.vanadium.wave.handler.ArgumentValidation
import dev.vanadium.wave.handler.CommandHandler
import dev.vanadium.wave.reflect.Command
import dev.vanadium.wave.runtime.Runtime

@Command("canvas")
class CanvasCommand : CommandHandler {

    var width: Double = 0.0
    var height: Double = 0.0
    var background: ArrayExpression? = null

    override fun validateArguments(args: HashMap<String, ExpressionNode>, runtime: Runtime): Array<ArgumentValidation> {
        return arrayOf(
            ArgumentValidation("width", ArgumentType.REQUIRED, LiteralExpression::class.java),
            ArgumentValidation("height", ArgumentType.REQUIRED, LiteralExpression::class.java),
            ArgumentValidation("background", ArgumentType.OPTIONAL, ArrayExpression::class.java)
        )
    }

    override fun preflight(args: HashMap<String, ExpressionNode>, runtime: Runtime, line: Int) {
        val width = (args["width"]!! as LiteralExpression<*>).ifTypeNot(DOUBLE_TYPE) {
            throw RuntimeException("Command argument \"width\" of command \"${name()}\" is expected to be a number")
        }.value as Double

        val height = (args["height"]!! as LiteralExpression<*>).ifTypeNot(DOUBLE_TYPE) {
            throw RuntimeException("Command argument \"height\" of command \"${name()}\" is expected to be a number")
        }.value as Double

        if (width <= 0 || height <= 0)
            throw RuntimeException("The canvas must be at least one pixel big. Error on line ${line}")

        this.width = width
        this.height = height

        if (args["background"] == null) {
            return
        }

        val background = (args["background"] as ArrayExpression).validateTypes()
        if (!background.checkAgainst(DOUBLE_TYPE, 3..4)) {
            throw RuntimeException("Command \"${name()}\" expected a number array of size 3 or 4 as optional argument \"background\" on line ${line}")
        }

        this.background = background
    }

    override fun command(
        args: HashMap<String, ExpressionNode>,
        canvas: Canvas,
        runtime: Runtime,
        line: Int
    ) {
        canvas.initialize(width.toInt(), height.toInt())

        if (background != null) {
            canvas.fill(RGBAColor.fromArray(background!!))
        }
    }
}