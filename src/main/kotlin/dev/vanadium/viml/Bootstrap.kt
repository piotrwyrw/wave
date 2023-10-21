package dev.vanadium.viml

import dev.vanadium.viml.analysis.syntactic.Parser
import dev.vanadium.viml.analysis.lexical.Tokenizer
import dev.vanadium.viml.runtime.Runtime
import dev.vanadium.viml.util.readFile
import java.io.File

fun main() {
    val tokenizer = Tokenizer(readFile(File("input.viml")))
    val parser = Parser(tokenizer)

    val script = parser.parseScript()
//    script.printAllNodes()

    val runtime = Runtime(script)

    runtime.run()
}