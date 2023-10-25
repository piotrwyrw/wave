package dev.vanadium.wave.util

import java.io.File

fun readFile(file: File): String {
    val lines = file.useLines {
        it.toList()
    }

    if (lines.isEmpty())
        return ""

    return lines.reduce { acc, s -> acc + "\n${s}" }
}
