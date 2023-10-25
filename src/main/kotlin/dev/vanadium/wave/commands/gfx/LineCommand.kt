package dev.vanadium.wave.commands.gfx

import dev.vanadium.wave.ArrayExpression
import dev.vanadium.wave.DOUBLE_TYPE
import dev.vanadium.wave.ExpressionNode
import dev.vanadium.wave.LiteralExpression
import dev.vanadium.wave.gfx.Canvas
import dev.vanadium.wave.handler.ArgumentType
import dev.vanadium.wave.handler.ArgumentValidation
import dev.vanadium.wave.handler.CommandHandler
import dev.vanadium.wave.reflect.Command
import dev.vanadium.wave.runtime.Runtime

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

    override fun preflight(args: HashMap<String, ExpressionNode>, runtime: Runtime, line: Int) {
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
        args: HashMap<String, ExpressionNode>,
        canvas: Canvas,
        runtime: Runtime,
        line: Int
    ) {
        canvas.line(fromX.toInt(), fromY.toInt(), toX.toInt(), toY.toInt())
    }
}