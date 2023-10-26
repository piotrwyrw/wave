package dev.vanadium.wave.analysis.lexical

enum class TokenType(val value: String? = null) {

    UNDEFINED,

    LPAREN("("),
    RPAREN(")"),
    LBRACKET("["),
    RBRACKET("]"),
    LCURLY("{"),
    RCURLY("}"),

    IDENTIFIER,

    NUMBER_LITERAL,
    STRING_LITERAL,

    COMMA(","),
    MINUS("-"),
    PLUS("+"),
    ASTERISK("*"),
    SLASH("/"),
    LGREATER(">"),
    RGREATER("<"),
    SEMICOLON(";"),
    DOLLARSIGN("$"),
    EQUALS("="),
    VBAR("|"),
    HAT("^"),
    AT("@"),

    // Complex tokens
    POINT_RIGHT("->"),
    APPROACHES("-->");
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