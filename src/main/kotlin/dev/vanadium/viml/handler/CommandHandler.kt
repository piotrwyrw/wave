package dev.vanadium.viml.handler

import dev.vanadium.viml.ExpressionNode
import dev.vanadium.viml.gfx.Canvas

interface CommandHandler {

    fun invoke(label: String, args: HashMap<String, ExpressionNode>) {
        command(label, args, Canvas.instance())
    }

    fun command(label: String, args: HashMap<String, ExpressionNode>, canvas: Canvas)

}