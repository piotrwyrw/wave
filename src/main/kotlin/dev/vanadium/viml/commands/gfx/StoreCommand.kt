package dev.vanadium.viml.commands.gfx

import dev.vanadium.viml.ExpressionNode
import dev.vanadium.viml.LiteralExpression
import dev.vanadium.viml.STRING_TYPE
import dev.vanadium.viml.gfx.Canvas
import dev.vanadium.viml.handler.ArgumentType
import dev.vanadium.viml.handler.ArgumentValidation
import dev.vanadium.viml.handler.CommandHandler
import dev.vanadium.viml.reflect.Command
import dev.vanadium.viml.runtime.Runtime

@Command("write")
class StoreCommand : CommandHandler {

    var file: String = ""

    override fun validateArguments(
        args: HashMap<String, ExpressionNode>,
        runtime: Runtime
    ): Array<ArgumentValidation> {
        return arrayOf(
            ArgumentValidation("file", ArgumentType.REQUIRED, LiteralExpression::class.java),
        )
    }

    override fun preflight(label: String, args: HashMap<String, ExpressionNode>, runtime: Runtime, line: Int) {
        file = (args["file"]!! as LiteralExpression<*>).ifTypeNot(STRING_TYPE) {
            throw RuntimeException("The command argument \"file\" of command \"${name()}\" is expected to be a string.")
        }.value as String
    }

    override fun command(
        label: String,
        args: HashMap<String, ExpressionNode>,
        canvas: Canvas,
        runtime: Runtime,
        line: Int
    ) {
        canvas.store(file)
    }
}