package dev.vanadium.viml

import dev.vanadium.viml.parse.Token
import dev.vanadium.viml.parse.Tokenizer

fun main() {
    val tokenizer = Tokenizer("int main(void) -> Unit")

    var token: Token?

    while (tokenizer.hasNext()) {
        println(tokenizer.nextToken()!!)
    }
}