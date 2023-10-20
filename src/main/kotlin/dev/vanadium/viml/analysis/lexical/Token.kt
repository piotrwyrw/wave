package dev.vanadium.viml.analysis.lexical

class Token(val type: TokenType, val value: String, val line: Int) {

    override fun toString(): String {
        return "Token(type=$type, value='$value', line=$line)"
    }

}

fun compareToken(token: Token?, identifier: String): Boolean {
    token ?: return false

    return token.value == identifier
}

fun compareToken(token: Token?, type: TokenType): Boolean {
    token ?: return false

    return token.type == type
}

fun undefinedToken(line: Int): Token {
    return Token(TokenType.UNDEFINED, "", line)
}