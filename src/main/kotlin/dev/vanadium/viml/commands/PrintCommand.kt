package dev.vanadium.viml.commands

import dev.vanadium.viml.ExpressionNode
import dev.vanadium.viml.LiteralExpression
import dev.vanadium.viml.gfx.Canvas
import dev.vanadium.viml.handler.ArgumentType
import dev.vanadium.viml.handler.ArgumentValidation
import dev.vanadium.viml.handler.CommandHandler
import dev.vanadium.viml.reflect.Command
import dev.vanadium.viml.runtime.Runtime

@Command("print")
class PrintCommand : CommandHandler {

    override fun validateArguments(args: HashMap<String, ExpressionNode>, runtime: Runtime): Array<ArgumentValidation> {
        return arrayOf(ArgumentValidation("default", ArgumentType.REQUIRED, LiteralExpression::class.java))
    }

    override fun command(label: String, args: HashMap<String, ExpressionNode>, canvas: Canvas, runtime: Runtime,line: Int) {
        if (args.size != 1 || !args.containsKey("default")) {
            println("The \"print\" command expects only the default parameter")
            return
        }

        if (args["default"] !is LiteralExpression<*>) {
            println(args["default"]!!.string())
            return
        }

        val lit = args["default"] as LiteralExpression<*>
        println(lit.value)
    }
}