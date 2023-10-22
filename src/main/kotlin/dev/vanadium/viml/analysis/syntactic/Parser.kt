package dev.vanadium.viml.analysis.syntactic

import dev.vanadium.viml.*
import dev.vanadium.viml.analysis.lexical.*
import dev.vanadium.viml.exception.SyntaxError
import java.lang.Double.parseDouble

class Parser(val tokenizer: Tokenizer) {

    var currentToken: Token = undefinedToken(0)
    var nextToken: Token = undefinedToken(0)

    lateinit var script: Script

    init {
        consume()
        consume()
    }

    fun consume() {
        currentToken = nextToken
        nextToken = tokenizer.nextToken() ?: undefinedToken(tokenizer.lineNumber)
    }

    fun parseScript(): Script {
        val nodes: ArrayList<Node> = arrayListOf()

        while (currentToken.type != TokenType.UNDEFINED) {
            nodes.add(parseNext())
        }

        this.script = Script(nodes)

        return this.script
    }

    fun parseNext(): Node {
        if (compareToken(currentToken, TokenType.IDENTIFIER)) {
            return parseCommand()
        }

        return parseExpression()
    }

    fun parseExpression(): ExpressionNode {
        val expr = parseSimpleExpression()

        if (!compareToken(currentToken, TokenType.POINT_RIGHT)) {
            return expr
        }

        consume() // Skip '->'

        val to = parseSimpleExpression()

        if (expr::class != to::class)
            throw SyntaxError("The interpolation expression expects same types on both sides, got ${expr.javaClass.simpleName} and ${to.javaClass.simpleName} on line ${currentToken.line}.")

        return InterpolationExpression(expr, to, expr.line)
    }

    fun parseSimpleExpression(): ExpressionNode {
        var left = parseMultiplicativeExpression()

        while (compareToken(currentToken, TokenType.PLUS) || compareToken(currentToken, TokenType.MINUS)) {
            val op = binaryOperationFromToken(currentToken)

            consume() // Skip the operator

            val right = parseMultiplicativeExpression()

            left = BinaryExpressionNode(left, right, op, right.line)
        }

        return left
    }

    fun parseMultiplicativeExpression(): ExpressionNode {
        var left = parseExpressionAtom()

        while (compareToken(currentToken, TokenType.ASTERISK) || compareToken(currentToken, TokenType.SLASH)) {
            val op = binaryOperationFromToken(currentToken)

            consume() // Skip the operator

            val right = parseExpressionAtom()

            left = BinaryExpressionNode(left, right, op, right.line)
        }

        return left
    }

    fun parseExpressionAtom(): ExpressionNode {
        if (compareToken(currentToken, TokenType.STRING_LITERAL)) {
            return parseStringLiteralExpression()
        }

        if (compareToken(currentToken, TokenType.NUMBER_LITERAL)) {
            return parseNumberLiteralExpression()
        }

        if (compareToken(currentToken, TokenType.LBRACKET)) {
            return parseArray()
        }

        if (compareToken(currentToken, TokenType.DOLLARSIGN)) {
            return parseVariableReferenceExpression()
        }

        if (compareToken(currentToken, TokenType.LPAREN)) {
            consume() // Skip '('
            val expr = parseExpression()
            if (!compareToken(currentToken, TokenType.RPAREN)) {
                throw SyntaxError("Expected ')' after expression, got ${currentToken.type} on line ${currentToken.line}")
            }
            consume() // Skip ')'
            return expr
        }

        throw SyntaxError("Unknown expression atom starting with ${currentToken.type} on line ${currentToken.line}")
    }

    fun parseNumberLiteralExpression(): LiteralExpression<Double> {
        if (!compareToken(currentToken, TokenType.NUMBER_LITERAL)) {
            throw SyntaxError("Expected integer literal, got ${currentToken.type} on line ${currentToken.line}")
        }

        val literal = LiteralExpression(parseDouble(currentToken.value), currentToken.line)

        consume()

        return literal
    }

    fun parseStringLiteralExpression(): LiteralExpression<String> {
        if (!compareToken(currentToken, TokenType.STRING_LITERAL)) {
            throw SyntaxError("Expected string literal, got ${currentToken.type} on line ${currentToken.line}")
        }

        val literal = LiteralExpression(currentToken.value, currentToken.line)

        consume()

        return literal
    }

    fun parseVariableReferenceExpression(): VariableReferenceExpression {
        if (!compareToken(currentToken, TokenType.DOLLARSIGN)) {
            throw SyntaxError("Expected '$' at the beginning of a variable reference, got ${currentToken.type} on line ${currentToken.line}")
        }

        val line = currentToken.line

        consume() // Skip '$'

        if (!compareToken(currentToken, TokenType.IDENTIFIER)) {
            throw SyntaxError("Expected identifier after '$', got ${currentToken.type} on line ${currentToken.line}")
        }

        val id = currentToken.value

        consume() // Skip variable ID

        return VariableReferenceExpression(id, line)
    }

    fun parseArray(): ArrayExpression {
        val expressions: ArrayList<ExpressionNode> = arrayListOf()

        if (!compareToken(currentToken, TokenType.LBRACKET)) {
            throw SyntaxError("Expected '[' at the beginning of an array, got ${currentToken.type} on line ${currentToken.line}")
        }

        val line = currentToken.line

        consume() // Skip '('

        while (!compareToken(currentToken, TokenType.RBRACKET)) {
            val expr = parseExpression()

            if (expr is ArrayExpression) {
                throw SyntaxError("Nested arrays are not allowed.")
            }

            expressions.add(expr)

            if (compareToken(currentToken, TokenType.COMMA)) {
                consume()
                continue
            }

            if (compareToken(currentToken, TokenType.RBRACKET)) {
                break
            }

            throw SyntaxError("Expected ']' or ',' and more expressions, got ${currentToken.type} on line ${currentToken.line}")
        }

        consume() // Skip ')'

        return ArrayExpression(expressions, line)
    }

    fun parseCommand(): CommandNode {
        if (!compareToken(currentToken, TokenType.IDENTIFIER)) {
            throw SyntaxError("Expected identifier at the beginning of a command, got ${currentToken.type} on line ${currentToken.line}")
        }

        val line = currentToken.line

        val label = currentToken.value

        consume() // Skip the command identifier

        val args = parseCommandArguments() // Parse the arguments

        return CommandNode(label, args, line)
    }

    fun parseCommandArguments(): HashMap<String, ExpressionNode> {
        val args: HashMap<String, ExpressionNode> = hashMapOf()

        // Maybe there are no parameters?
        if (compareToken(currentToken, TokenType.SEMICOLON)) {
            consume() // Skip ';'
            return args
        }

        // First, try parsing a default parameter
        if (!compareToken(currentToken, TokenType.IDENTIFIER)) {
            val expr = parseExpression()
            args.put("default", expr)
        }

        // Parse regular parameters
        while (true) {
            // The semicolon marks the end of the argument sequence
            if (compareToken(currentToken, TokenType.SEMICOLON)) {
                consume() // Skip ';'
                break
            }

            if (!compareToken(currentToken, TokenType.IDENTIFIER)) {
                throw SyntaxError("Expected ';' or more arguments, got ${currentToken.type} on line ${currentToken.line}.")
            }

            val label = currentToken.value

            if (label == "default") {
                throw SyntaxError("A command argument may not be explicitly labeled 'default'. Error on line ${currentToken.line}.")
            }

            consume() // Skip arg label

            val expr = parseExpression()

            args.put(label, expr)
        }

        return args
    }

}