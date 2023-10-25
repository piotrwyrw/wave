package dev.vanadium.viml.commands.gfx

import dev.vanadium.viml.ArrayExpression
import dev.vanadium.viml.DOUBLE_TYPE
import dev.vanadium.viml.ExpressionNode
import dev.vanadium.viml.gfx.Canvas
import dev.vanadium.viml.gfx.RGBAColor
import dev.vanadium.viml.handler.ArgumentType
import dev.vanadium.viml.handler.ArgumentValidation
import dev.vanadium.viml.handler.CommandHandler
import dev.vanadium.viml.reflect.Command
import dev.vanadium.viml.runtime.Runtime

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

    override fun preflight(label: String, args: HashMap<String, ExpressionNode>, runtime: Runtime, line: Int) {
        val array = args["default"]!! as ArrayExpression

        if (!array.checkAgainst(DOUBLE_TYPE, 3 .. 4)) {
            throw RuntimeException("The default argument of command \"${name()}\" is expected to be a number array of length 3 or 4. Error on line ${line}}.")
        }

        this.color = array
    }

    override fun command(
        label: String,
        args: HashMap<String, ExpressionNode>,
        canvas: Canvas,
        runtime: Runtime,
        line: Int
    ) {
        canvas.color(RGBAColor.fromArray(color!!))
    }
}