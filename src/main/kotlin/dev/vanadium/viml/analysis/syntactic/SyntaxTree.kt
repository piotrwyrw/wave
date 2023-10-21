package dev.vanadium.viml.analysis.syntactic

import dev.vanadium.viml.analysis.lexical.Token
import dev.vanadium.viml.analysis.lexical.TokenType
import dev.vanadium.viml.exception.SyntaxError

class Script(val nodes: List<Node>) {

    fun printAllNodes() {
        nodes.forEach {
            it.print(0)
        }
    }

}

/**
 * Superclass of all nodes ever
 */
abstract class Node {
    abstract fun print(indent: Int)

    protected fun indentation(indent: Int): String {
        return " ".repeat(indent * 4)
    }

}

/**
 * The base class for all nodes involved in flow control and
 * commanding of the underlying graphics engine
 */
abstract class StatementNode : Node()

/**
 * Base class for all expression nodes (arrays, arithmetic ops, ...)
 */
abstract class ExpressionNode : Node()


class StringLiteralExpression(
    val value: String
) : ExpressionNode() {
    override fun print(indent: Int) {
        println(indentation(indent) + "String Literal \"${value}\"")
    }

}

class NumberLiteralExpression(
    val value: Double
) : ExpressionNode() {
    override fun print(indent: Int) {
        println(indentation(indent) + "Number Literal ${value}")
    }
}

class ArrayExpression(
    val value: List<ExpressionNode>
) : ExpressionNode() {
    override fun print(indent: Int) {
        println(indentation(indent) + "Array Expression:")
        value.forEachIndexed { index, expr ->
            println(indentation(indent + 1) + "${index}:")
            expr.print(indent + 2)
        }
    }
}

class VariableReferenceExpression(
    val id: String
) : ExpressionNode() {
    override fun print(indent: Int) {
        println(indentation(indent) + "Variable: ${id}")
    }
}

class InterpolationExpression(
    val from: ExpressionNode,
    val to: ExpressionNode
) : ExpressionNode() {
    override fun print(indent: Int) {
        println(indentation(indent) + "Interpolation:")
        println(indentation(indent + 1) + "From:")
        from.print(indent + 2)

        println(indentation(indent + 1) + "To:")
        to.print(indent + 2)
    }
}

enum class BinaryOperation {
    ADD,
    SUBTRACT,
    MULTIPLY,
    DIVIDE
}

fun binaryOperationFromToken(token: Token): BinaryOperation {
    return when (token.type) {
        TokenType.PLUS -> BinaryOperation.ADD
        TokenType.SLASH -> BinaryOperation.DIVIDE
        TokenType.MINUS -> BinaryOperation.SUBTRACT
        TokenType.ASTERISK -> BinaryOperation.MULTIPLY
        else -> throw SyntaxError("Unknown binary operator ${token.type} on line ${token.line}")
    }
}

class BinaryExpressionNode(
    val left: ExpressionNode,
    val right: ExpressionNode,
    val operator: BinaryOperation
) : ExpressionNode() {
    override fun print(indent: Int) {
        println(indentation(indent) + "Binary operation (${operator}):")
        println(indentation(indent + 1) + "Left:")
        left.print(indent + 2)

        println(indentation(indent + 1) + "Right")
        right.print(indent + 2)
    }
}

class CommandNode(
    val label: String,
    val args: HashMap<String, ExpressionNode>
) : StatementNode() {
    override fun print(indent: Int) {
        println(indentation(indent) + "Command Node \"${label}\":")
        args.forEach { (t, u) ->
            println(indentation(indent + 1) + "${t}:")
            u.print(indent + 2)
        }
    }
}