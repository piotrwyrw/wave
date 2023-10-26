package dev.vanadium.wave

import dev.vanadium.wave.analysis.lexical.Tokenizer
import dev.vanadium.wave.analysis.syntactic.Parser
import dev.vanadium.wave.runtime.Runtime
import dev.vanadium.wave.util.readFile
import java.io.File

fun main() {
    val tokenizer = Tokenizer(readFile(File("input.wave")))
    val parser = Parser(tokenizer)

    val script = parser.parseScript()

    val runtime = Runtime(script)

    runtime.run()
}