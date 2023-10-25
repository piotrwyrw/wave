package dev.vanadium.wave.commands.gfx

import dev.vanadium.wave.ArrayExpression
import dev.vanadium.wave.DOUBLE_TYPE
import dev.vanadium.wave.ExpressionNode
import dev.vanadium.wave.gfx.Canvas
import dev.vanadium.wave.gfx.RGBAColor
import dev.vanadium.wave.handler.ArgumentType
import dev.vanadium.wave.handler.ArgumentValidation
import dev.vanadium.wave.handler.CommandHandler
import dev.vanadium.wave.reflect.Command
import dev.vanadium.wave.runtime.Runtime

@Command("color")
class ColorCommand : CommandHandler {

    var color: ArrayExpression? = null

    override fun validateArguments(
        args: HashMap<String, ExpressionNode>,
        runtime: Runtime
    ): Array<ArgumentValidation>? {
        return arrayOf(
            ArgumentValidation("default", ArgumentType.REQUIRED, ArrayExpression::class.java)
        )
    }

    override fun preflight(args: HashMap<String, ExpressionNode>, runtime: Runtime, line: Int) {
        val array = args["default"]!! as ArrayExpression

        if (!array.checkAgainst(DOUBLE_TYPE, 3 .. 4)) {
            throw RuntimeException("The default argument of command \"${name()}\" is expected to be a number array of length 3 or 4. Error on line ${line}}.")
        }

        this.color = array
    }

    override fun command(
        args: HashMap<String, ExpressionNode>,
        canvas: Canvas,
        runtime: Runtime,
        line: Int
    ) {
        canvas.color(RGBAColor.fromArray(color!!))
    }
}