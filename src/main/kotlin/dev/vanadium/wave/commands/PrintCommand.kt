package dev.vanadium.wave.commands

import dev.vanadium.wave.ExpressionNode
import dev.vanadium.wave.LiteralExpression
import dev.vanadium.wave.gfx.Canvas
import dev.vanadium.wave.handler.ArgumentType
import dev.vanadium.wave.handler.ArgumentValidation
import dev.vanadium.wave.handler.CommandHandler
import dev.vanadium.wave.reflect.Command
import dev.vanadium.wave.runtime.Runtime

@Command("print")
class PrintCommand : CommandHandler {

    override fun validateArguments(args: HashMap<String, ExpressionNode>, runtime: Runtime): Array<ArgumentValidation> {
        return arrayOf(ArgumentValidation("default", ArgumentType.REQUIRED, LiteralExpression::class.java))
    }

    override fun command(args: HashMap<String, ExpressionNode>, canvas: Canvas, runtime: Runtime, line: Int) {
        if (args["default"]!! !is LiteralExpression<*>) {
            println(args["default"]!!.string())
            return
        }

        val lit = args["default"] as LiteralExpression<*>
        println(lit.value)
    }
}