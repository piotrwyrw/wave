package dev.vanadium.wave.commands.gfx

import dev.vanadium.wave.ExpressionNode
import dev.vanadium.wave.LiteralExpression
import dev.vanadium.wave.STRING_TYPE
import dev.vanadium.wave.gfx.Canvas
import dev.vanadium.wave.handler.ArgumentType
import dev.vanadium.wave.handler.ArgumentValidation
import dev.vanadium.wave.handler.CommandHandler
import dev.vanadium.wave.reflect.Command
import dev.vanadium.wave.runtime.Runtime

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

    override fun preflight(args: HashMap<String, ExpressionNode>, runtime: Runtime, line: Int) {
        file = (args["file"]!! as LiteralExpression<*>).ifTypeNot(STRING_TYPE) {
            throw RuntimeException("The command argument \"file\" of command \"${name()}\" is expected to be a string.")
        }.value as String
    }

    override fun command(
        args: HashMap<String, ExpressionNode>,
        canvas: Canvas,
        runtime: Runtime,
        line: Int
    ) {
        canvas.store(file)
    }
}