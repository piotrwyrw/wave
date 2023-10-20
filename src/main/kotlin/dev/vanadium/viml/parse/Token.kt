package dev.vanadium.viml.parse

class Token(val type: TokenType, val value: String, val line: Int) {

    fun cmpIdentifier(id: String): Boolean {
        return type == TokenType.IDENTIFIER && this.value == id
    }

    fun cmpType(type: TokenType): Boolean {
        return this.type == type
    }

    override fun toString(): String {
        return "Token(type=$type, value='$value', line=$line)"
    }

}