package dev.vanadium.viml.commands.gfx

import dev.vanadium.viml.ArrayExpression
import dev.vanadium.viml.DOUBLE_TYPE
import dev.vanadium.viml.ExpressionNode
import dev.vanadium.viml.LiteralExpression
import dev.vanadium.viml.gfx.Canvas
import dev.vanadium.viml.handler.ArgumentType
import dev.vanadium.viml.handler.ArgumentValidation
import dev.vanadium.viml.handler.CommandHandler
import dev.vanadium.viml.reflect.Command
import dev.vanadium.viml.runtime.Runtime

@Command("line")
class LineCommand : CommandHandler {

    var fromX: Double = 0.0
    var fromY: Double = 0.0

    var toX: Double = 0.0
    var toY: Double = 0.0

    override fun validateArguments(
        args: HashMap<String, ExpressionNode>,
        runtime: Runtime
    ): Array<ArgumentValidation> {
        return arrayOf(
            ArgumentValidation("from", ArgumentType.REQUIRED, ArrayExpression::class.java),
            ArgumentValidation("to", ArgumentType.REQUIRED, ArrayExpression::class.java)
        )
    }

    override fun preflight(label: String, args: HashMap<String, ExpressionNode>, runtime: Runtime, line: Int) {
        val from = args["from"]!! as ArrayExpression
        val to = args["to"]!! as ArrayExpression

        if (!from.checkAgainst(DOUBLE_TYPE, 2 .. 2)) {
            throw RuntimeException("The \"from\" array of command \"${name()}\" has to be a number array and of length 2.")
        }

        if (!to.checkAgainst(DOUBLE_TYPE, 2 .. 2)) {
            throw RuntimeException("The \"to\" array of command \"${name()}\" has to be a number array and of length 2.")
        }

        fromX = (from.value.get(0) as LiteralExpression<*>).value as Double
        fromY = (from.value.get(1) as LiteralExpression<*>).value as Double

        toX = (to.value.get(0) as LiteralExpression<*>).value as Double
        toY = (to.value.get(1) as LiteralExpression<*>).value as Double
    }

    override fun command(
        label: String,
        args: HashMap<String, ExpressionNode>,
        canvas: Canvas,
        runtime: Runtime,
        line: Int
    ) {
        canvas.line(fromX.toInt(), fromY.toInt(), toX.toInt(), toY.toInt())
    }
}