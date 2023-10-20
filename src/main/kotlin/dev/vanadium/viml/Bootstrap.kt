package dev.vanadium.viml

import dev.vanadium.viml.analysis.syntactic.Parser
import dev.vanadium.viml.analysis.lexical.Tokenizer

fun main() {
    val tokenizer = Tokenizer("line (1, 2) -> (4, 3);")
    val parser = Parser(tokenizer)

    val node = parser.parseScript()
    node.printAllNodes()
}