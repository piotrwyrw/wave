package dev.vanadium.viml

import dev.vanadium.viml.parse.Parser
import dev.vanadium.viml.parse.Tokenizer

fun main() {
    val tokenizer = Tokenizer("(1, 2, \"Hello, World!\")")
    val parser = Parser(tokenizer)

    val node = parser.parseScript()
}