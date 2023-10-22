package dev.vanadium.viml.handler

import dev.vanadium.viml.ExpressionNode
import dev.vanadium.viml.gfx.Canvas
import dev.vanadium.viml.runtime.Runtime

interface CommandHandler {

    fun invoke(label: String, args: HashMap<String, ExpressionNode>, runtime: Runtime, line: Int) {
        command(label, args, Canvas.instance(), runtime, line)
    }

    open fun validateArguments(args: HashMap<String, ExpressionNode>, runtime: Runtime): Array<ArgumentValidation>? = null
    open fun preflight(label: String, args: HashMap<String, ExpressionNode>, runtime: Runtime, line: Int) {}
    fun command(label: String, args: HashMap<String, ExpressionNode>, canvas: Canvas, runtime: Runtime, line: Int)

}