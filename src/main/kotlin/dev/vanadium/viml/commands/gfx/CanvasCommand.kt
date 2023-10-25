package dev.vanadium.viml.commands.gfx

import dev.vanadium.viml.ArrayExpression
import dev.vanadium.viml.DOUBLE_TYPE
import dev.vanadium.viml.ExpressionNode
import dev.vanadium.viml.LiteralExpression
import dev.vanadium.viml.gfx.Canvas
import dev.vanadium.viml.gfx.RGBAColor
import dev.vanadium.viml.handler.ArgumentType
import dev.vanadium.viml.handler.ArgumentValidation
import dev.vanadium.viml.handler.CommandHandler
import dev.vanadium.viml.reflect.Command
import dev.vanadium.viml.runtime.Runtime

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

    override fun preflight(label: String, args: HashMap<String, ExpressionNode>, runtime: Runtime, line: Int) {
        val width = (args["width"]!! as LiteralExpression<*>).ifTypeNot(DOUBLE_TYPE) {
            throw RuntimeException("Command argument \"width\" of command \"${label}\" is expected to be a number")
        }.value as Double

        val height = (args["height"]!! as LiteralExpression<*>).ifTypeNot(DOUBLE_TYPE) {
            throw RuntimeException("Command argument \"height\" of command \"${label}\" is expected to be a number")
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
            throw RuntimeException("Command \"${label}\" expected an array of size 3 or 4 as optional argument \"background\" on line ${line}")
        }

        this.background = background
    }

    override fun command(
        label: String,
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