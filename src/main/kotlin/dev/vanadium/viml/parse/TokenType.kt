package dev.vanadium.viml.parse

enum class TokenType(val value: String? = null) {

    UNDEFINED(),

    LPAREN("("),
    RPAREN(")"),

    IDENTIFIER,

    INTEGER_LITERAL,
    STRING_LITERAL,

    COMMA(","),
    MINUS("-"),
    LGREATER(">"),
    RGREATER("<"),

    // Complex tokens
    POINT_RIGHT("->")

}

fun findMatchingType(str: String): TokenType? {
    var match: TokenType? = null

    for (type in TokenType.entries) {
        if (type.value == null)
            continue

        if (str.startsWith(type.value) && ((match != null && type.value.length > match.value!!.length) || match == null))
            match = type
    }

    return match
}