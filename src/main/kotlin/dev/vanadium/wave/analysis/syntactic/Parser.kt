package dev.vanadium.wave.analysis.syntactic

import dev.vanadium.wave.*
import dev.vanadium.wave.analysis.lexical.*
import dev.vanadium.wave.exception.SyntaxError
import java.lang.Double.compare
import java.lang.Double.parseDouble

class Parser(val tokenizer: Tokenizer) {

    var currentToken: Token = undefinedToken(0)
    var nextToken: Token = undefinedToken(0)

    val reserved: Array<String> = arrayOf("invariable", "fun", "repeat")

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
        if (compareToken(currentToken, "repeat")) {
            return parseRepeat()
        }

        if (compareToken(currentToken, "fun")) {
            return parseFunctionDefinition()
        }

        if ((compareToken(currentToken, TokenType.IDENTIFIER) && !compareToken(
                nextToken,
                TokenType.EQUALS
            )) && !reserved.contains(currentToken.value)
        ) {
            return parseCommand()
        }

        return parseExpression()
    }

    fun parseFunctionDefinition(): FunctionDefinitionNode {
        if (!compareToken(currentToken, "fun")) {
            throw RuntimeException("Expected 'fun' at the beginning of a function definition, got ${currentToken.type} on line ${currentToken.line}")
        }

        val line = currentToken.line

        consume() // SKip 'fun'

        if (!compareToken(currentToken, TokenType.IDENTIFIER)) {
            throw RuntimeException("Expected identifier after 'fun', got ${currentToken.type} on line ${currentToken.line}")
        }

        val id = currentToken

        consume() // Skip id

        if (!compareToken(currentToken, TokenType.LCURLY)) {
            throw RuntimeException("Expected '{' after function identifier, got ${currentToken.type} on line ${currentToken.line}")
        }

        val block = parseBlock()

        return FunctionDefinitionNode(id, block, line)
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

        while (compareToken(currentToken, TokenType.ASTERISK) || compareToken(
                currentToken,
                TokenType.SLASH
            ) || compareToken(currentToken, TokenType.HAT) || compareToken(currentToken, TokenType.AT)
        ) {
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

        if (compareToken(currentToken, TokenType.IDENTIFIER)) {
            return parseVariableAssignment()
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

        if (compareToken(currentToken, TokenType.VBAR)) {
            return parseUnaryMagnitudeOperation()
        }

        throw SyntaxError("Unknown expression atom starting with ${currentToken.type} on line ${currentToken.line}")
    }

    fun parseUnaryMagnitudeOperation(): UnaryOperationNode {
        if (!compareToken(currentToken, TokenType.VBAR)) {
            throw SyntaxError("Expected vertical bar at the beginning of a magnitude operator, got ${currentToken.type} on line ${currentToken.line}")
        }

        val line = currentToken.line

        consume() // Skip first '|'

        val expr = parseExpression()

        if (!compareToken(currentToken, TokenType.VBAR)) {
            throw SyntaxError("Expected vertical bar at the end of a magnitude operator, got ${currentToken.type} on line ${currentToken.line}")
        }

        consume() // Skip second '|'

        return UnaryOperationNode(expr, UnaryOperation.MAGNITUDE, line)
    }

    fun parseNumberLiteralExpression(): LiteralExpression<Double> {
        if (!compareToken(currentToken, TokenType.NUMBER_LITERAL)) {
            throw SyntaxError("Expected integer literal, got ${currentToken.type} on line ${currentToken.line}")
        }

        val literal = LiteralExpression<Double>(parseDouble(currentToken.value), currentToken.line)

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

    fun parseVariableAssignment(): VariableAssignment {
        var invariable = false

        if (compareToken(currentToken, "invariable")) {
            invariable = true
            consume(); // Skip the qualifier
        }

        if (!compareToken(currentToken, TokenType.IDENTIFIER)) {
            throw SyntaxError("Expected an identifier at the beginning of a variable reference, got ${currentToken.type} on line ${currentToken.line}")
        }

        val line = currentToken.line
        val id = currentToken.value

        consume() // Skip id

        if (!compareToken(currentToken, TokenType.EQUALS)) {
            throw RuntimeException("Expected '=' after identifier, got ${currentToken.type} on line ${currentToken.line}")
        }

        consume() // Skip '='

        val expr = parseExpression()

        return VariableAssignment(id, expr, invariable, line)
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

    fun parseRepeat(): RepeatNode {
        if (!compareToken(currentToken, "repeat")) {
            throw RuntimeException("Epxected 'repeat' at the beginning of a repeat statement, got ${currentToken.type} on line ${currentToken.line}")
        }

        val line = currentToken.line

        consume() // Skip 'repeat'

        if (!compareToken(currentToken, TokenType.IDENTIFIER)) {
            throw RuntimeException("Expected variable identifier after 'repeat', got ${currentToken.type} on line ${currentToken.line}")
        }

        val variable = currentToken

        consume() // Skip variable identifier

        if (!compareToken(currentToken, TokenType.APPROACHES)) {
            throw RuntimeException("Expected '-->' operator after variable name, got ${currentToken.type} on line ${currentToken.line}")
        }

        consume() // Skip '-->'

        val target = parseExpression()

        if (!compareToken(currentToken, TokenType.LCURLY)) {
            throw RuntimeException("Expected '{' after repeat expression, got ${currentToken.type} on line ${currentToken.line}")
        }

        val block = parseBlock()

        return RepeatNode(target, block, variable, line)
    }

    fun parseBlock(): BlockNode {
        if (!compareToken(currentToken, TokenType.LCURLY)) {
            throw RuntimeException("Expected '{' at the beginning of a block statement, got ${currentToken.type} on line ${currentToken.line}")
        }

        val line = currentToken.line

        consume() // Skip '{'

        val nodes: ArrayList<Node> = arrayListOf()

        while (!compareToken(currentToken, TokenType.RCURLY) && !compareToken(currentToken, TokenType.UNDEFINED)) {
            nodes.add(parseNext())
        }

        if (!compareToken(currentToken, TokenType.RCURLY)) {
            throw RuntimeException("Reached end of file while parsing block statement starting on line ${line}.")
        }

        consume() // SKip '}'

        return BlockNode(nodes, line)
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