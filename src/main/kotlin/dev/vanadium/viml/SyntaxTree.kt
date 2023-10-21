package dev.vanadium.viml

import dev.vanadium.viml.analysis.lexical.Token
import dev.vanadium.viml.analysis.lexical.TokenType
import dev.vanadium.viml.exception.SyntaxError
import dev.vanadium.viml.runtime.Runtime

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
abstract class Node(val line: Int) {
    abstract fun print(indent: Int)

    protected fun indentation(indent: Int): String {
        return " ".repeat(indent * 4)
    }

}

/**
 * The base class for all nodes involved in flow control and
 * commanding of the underlying graphics engine
 */
abstract class StatementNode(line: Int) : Node(line)

/**
 * Base class for all expression nodes (arrays, arithmetic ops, ...)
 */
abstract class ExpressionNode(line: Int) : Node(line) {

    /**
     * Reduces the expression to its most atomic form.
     * By default, assume the expression is already atomic
     */
    open fun reduceToAtomic(runtime: Runtime): ExpressionNode = this
}


class StringLiteralExpression(
    val value: String,
    line: Int
) : ExpressionNode(line) {
    override fun print(indent: Int) {
        println(indentation(indent) + "String Literal \"${value}\"")
    }

}

class NumberLiteralExpression(
    val value: Double,
    line: Int
) : ExpressionNode(line) {
    override fun print(indent: Int) {
        println(indentation(indent) + "Number Literal ${value}")
    }
}

class ArrayExpression(
    val value: List<ExpressionNode>,
    line: Int
) : ExpressionNode(line) {
    override fun print(indent: Int) {
        println(indentation(indent) + "Array Expression:")
        value.forEachIndexed { index, expr ->
            println(indentation(indent + 1) + "${index}:")
            expr.print(indent + 2)
        }
    }
}

class VariableReferenceExpression(
    val id: String,
    line: Int
) : ExpressionNode(line) {
    override fun print(indent: Int) {
        println(indentation(indent) + "Variable: ${id}")
    }

    override fun reduceToAtomic(runtime: Runtime): ExpressionNode = runtime.findVariable(id).value
}

class InterpolationExpression(
    val from: ExpressionNode,
    val to: ExpressionNode,
    line: Int
) : ExpressionNode(line) {
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
    DIVIDE;

    fun applyNumerically(left: Double, right: Double): Double {
        return when (this) {
            ADD -> left + right
            SUBTRACT -> left - right
            MULTIPLY -> left * right
            DIVIDE -> left / right
        }
    }

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
    val operator: BinaryOperation,
    line: Int
) : ExpressionNode(line) {
    override fun print(indent: Int) {
        println(indentation(indent) + "Binary operation (${operator}):")
        println(indentation(indent + 1) + "Left:")
        left.print(indent + 2)

        println(indentation(indent + 1) + "Right")
        right.print(indent + 2)
    }

    override fun reduceToAtomic(runtime: Runtime): ExpressionNode {
        val leftAtomic = left.reduceToAtomic(runtime)
        val rightAtomic = right.reduceToAtomic(runtime)

        // Handle string operations upfront
        if (leftAtomic is StringLiteralExpression || rightAtomic is StringLiteralExpression) {
            if (operator != BinaryOperation.ADD) {
                throw RuntimeException("Only an addition may be performed on an expression involving a string. Attempted ${operator} on line ${line}.")
            }

            val leftValue: String = if (leftAtomic is StringLiteralExpression) (leftAtomic as StringLiteralExpression).value else (leftAtomic as NumberLiteralExpression).value.toString()
            val rightValue: String = if (rightAtomic is StringLiteralExpression) (rightAtomic as StringLiteralExpression).value else (rightAtomic as NumberLiteralExpression).value.toString()

            return StringLiteralExpression(
                leftValue + rightValue,
                line
            )
        }

        if (leftAtomic !is NumberLiteralExpression || rightAtomic !is NumberLiteralExpression) {
            throw RuntimeException("Illegal types for a binary operation: ${leftAtomic::class.simpleName} and ${rightAtomic::class.simpleName} on line ${line}")
        }

        return NumberLiteralExpression(operator.applyNumerically(leftAtomic.value, rightAtomic.value), line)
    }
}

class CommandNode(
    val label: String,
    val args: HashMap<String, ExpressionNode>,
    line: Int
) : StatementNode(line) {
    override fun print(indent: Int) {
        println(indentation(indent) + "Command \"${label}\":")
        args.forEach { (t, u) ->
            println(indentation(indent + 1) + "${t}:")
            u.print(indent + 2)
        }
        args.ifEmpty {
            println(indentation(indent + 1) + "- No arguments -")
        }
    }
}