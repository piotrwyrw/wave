package dev.vanadium.wave

import dev.vanadium.wave.analysis.lexical.Tokenizer
import dev.vanadium.wave.analysis.syntactic.Parser
import dev.vanadium.wave.exception.SyntaxError
import dev.vanadium.wave.runtime.Runtime
import dev.vanadium.wave.util.readFile
import java.io.File

fun main() {
    val tokenizer = Tokenizer(readFile(File("input.wave")))
    val parser = Parser(tokenizer)
    val script: Script
    try {
        script = parser.parseScript()
    } catch (excpt: SyntaxError) {
        println("[Parser] ${excpt.message}")
        return
    }
    script.printAllNodes()
    val runtime = Runtime(script)
    try {
        runtime.run()
    } catch (excpt: Exception) {
        println("[Runtime] ${excpt.message}")
    }
}