package dev.vanadium.viml.cmd

import dev.vanadium.viml.analysis.syntactic.ExpressionNode

interface CommandHandler {

    fun command(label: String, args: HashMap<String, ExpressionNode>)

}