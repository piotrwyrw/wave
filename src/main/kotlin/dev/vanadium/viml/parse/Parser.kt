package dev.vanadium.viml.parse

import dev.vanadium.viml.exception.SyntaxError

class Parser(val tokenizer: Tokenizer) {

    var currentToken: Token = undefinedToken(0)
    var nextToken: Token = undefinedToken(0)

    var script: Script? = null

    init {
        consume()
        consume()
    }

    fun consume() {
        currentToken = nextToken
        nextToken = tokenizer.nextToken() ?: undefinedToken(tokenizer.lineNumber)
    }

    fun parseScript(): Script? {
        val nodes: ArrayList<Node> = arrayListOf()

        while (tokenizer.hasNext()) {
            nodes.add(parseNext())
        }

        this.script = Script(nodes)

        return this.script
    }

    fun parseNext(): Node {
        return parseExpression()
    }

    fun parseExpression(): ExpressionNode {
        if (compareToken(currentToken, TokenType.INTEGER_LITERAL)) {
            return parseIntegerLiteralExpression()
        }

        if (compareToken(currentToken, TokenType.STRING_LITERAL)) {
            return parseStringLiteralExpression()
        }

        if (compareToken(currentToken, TokenType.LPAREN)) {
            return parseArray()
        }

        throw SyntaxError("Unknown expression starting with ${currentToken.type} on line ${currentToken.line}")
    }

    fun parseIntegerLiteralExpression(): IntegerLiteralExpression {
        if (!compareToken(currentToken, TokenType.INTEGER_LITERAL)) {
            throw SyntaxError("Expected integer literal, got ${currentToken.type} on line ${currentToken.line}")
        }

        val literal = IntegerLiteralExpression(Integer.parseInt(currentToken.value))

        consume()

        return literal
    }

    fun parseStringLiteralExpression(): StringLiteralExpression {
        if (!compareToken(currentToken, TokenType.STRING_LITERAL)) {
            throw SyntaxError("Expected string literal, got ${currentToken.type} on line ${currentToken.line}")
        }

        val literal = StringLiteralExpression(currentToken.value)

        consume()

        return literal
    }

    fun parseArray(): ArrayExpression {
        val expressions: ArrayList<ExpressionNode> = arrayListOf()

        if (!compareToken(currentToken, TokenType.LPAREN)) {
            throw SyntaxError("Expected '(' at the beginning of an array, got ${currentToken.type} on line ${currentToken!!.line}")
        }

        consume() // Skip '('

        while (!compareToken(currentToken, TokenType.RPAREN)) {
            val expr = parseExpression()

            if (expr is ArrayExpression) {
                throw SyntaxError("Nested arrays are not allowed.")
            }

            expressions.add(expr)

            if (compareToken(currentToken, TokenType.COMMA)) {
                consume()
                continue
            }

            if (compareToken(currentToken, TokenType.RPAREN)) {
                break
            }

            throw SyntaxError("Expected ')' or ',' and more expressions, got ${currentToken.type} on line ${currentToken.line}")
        }

        consume() // Skip ')'

        return ArrayExpression(expressions)
    }

}