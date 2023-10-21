package dev.vanadium.viml.commands

import dev.vanadium.viml.ExpressionNode
import dev.vanadium.viml.NumberLiteralExpression
import dev.vanadium.viml.StringLiteralExpression
import dev.vanadium.viml.gfx.Canvas
import dev.vanadium.viml.handler.CommandHandler
import dev.vanadium.viml.reflect.Command

@Command("print")
class PrintCommand : CommandHandler {
    override fun command(label: String, args: HashMap<String, ExpressionNode>, canvas: Canvas) {
        if (args.size != 1 || !args.containsKey("default")) {
            println("The \"print\" command expects only the default parameter")
            return
        }

        if (args["default"] is StringLiteralExpression)
            println((args["default"] as StringLiteralExpression).value)
        else if (args["default"] is NumberLiteralExpression)
            println((args["default"] as NumberLiteralExpression).value)
        else
            println("[Complex Object]")
    }
}