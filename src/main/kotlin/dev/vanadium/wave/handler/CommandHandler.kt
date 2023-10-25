package dev.vanadium.wave.handler

import dev.vanadium.wave.ExpressionNode
import dev.vanadium.wave.gfx.Canvas
import dev.vanadium.wave.reflect.Command
import dev.vanadium.wave.runtime.Runtime

interface CommandHandler {

    fun invoke(args: HashMap<String, ExpressionNode>, runtime: Runtime, line: Int) {
        command(args, Canvas.instance(), runtime, line)
    }

    open fun validateArguments(args: HashMap<String, ExpressionNode>, runtime: Runtime): Array<ArgumentValidation>? = null
    open fun preflight(args: HashMap<String, ExpressionNode>, runtime: Runtime, line: Int) {}
    fun command(args: HashMap<String, ExpressionNode>, canvas: Canvas, runtime: Runtime, line: Int)

    fun name(): String {
        val cmd: Command = this::class.java.getAnnotation(Command::class.java) ?: return "<undefined>"
        return cmd.label
    }

}