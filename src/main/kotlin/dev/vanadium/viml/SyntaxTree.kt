package dev.vanadium.viml

import dev.vanadium.viml.analysis.lexical.Token
import dev.vanadium.viml.analysis.lexical.TokenType
import dev.vanadium.viml.exception.SyntaxError
import dev.vanadium.viml.runtime.Runtime

val DOUBLE_TYPE = java.lang.Double::class.java
val STRING_TYPE = java.lang.String::class.java

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

    fun operation(op: BinaryOperation, another: ExpressionNode): ExpressionNode {
        val allOps = allOperations(another, op)
        if (allOps != null) {
            return allOps
        }

        return when (op) {
            BinaryOperation.ADD -> add(another)
            BinaryOperation.SUBTRACT -> subtract(another)
            BinaryOperation.MULTIPLY -> multiply(another)
            BinaryOperation.DIVIDE -> divide(another)
        }
    }

    open fun allOperations(another: ExpressionNode, operator: BinaryOperation): ExpressionNode? = null
    open fun add(another: ExpressionNode): ExpressionNode =
        throw RuntimeException("Illegal addition operation on ${this::class.java.simpleName} on line ${line}")

    open fun subtract(another: ExpressionNode): ExpressionNode =
        throw RuntimeException("Illegal subtraction operation on ${this::class.java.simpleName} on line ${line}")

    open fun multiply(another: ExpressionNode): ExpressionNode =
        throw RuntimeException("Illegal multiplication operation on ${this::class.java.simpleName} on line ${line}")

    open fun divide(another: ExpressionNode): ExpressionNode =
        throw RuntimeException("Illegal division operation on ${this::class.java.simpleName} on line ${line}")

    open fun string(): String =
        throw RuntimeException("The object \"${this::class.java.simpleName}\" does not support string conversions. Attempted on line ${line}")

}


class LiteralExpression<T : Any>(
    val value: T,
    line: Int
) : ExpressionNode(line) {
    override fun print(indent: Int) {
        println(indentation(indent) + "Literal \"${value}\"")
    }

    override fun allOperations(another: ExpressionNode, operator: BinaryOperation): ExpressionNode? {
        if (another !is LiteralExpression<*> && value !is String) throw RuntimeException("Cannot add a non-literal to a literal on line ${line}")
        return null
    }

    override fun string(): String {
        return value.toString()
    }

    fun ifTypeNot(clazz: Class<*>, callback: (actualType: Class<*>) -> Unit): LiteralExpression<T> {
        val type = value::class.java

        if (type.isAssignableFrom(clazz))
            return this

        callback(type)
        return this
    }

    override fun add(another: ExpressionNode): ExpressionNode {
        val anotherValue = if (another !is LiteralExpression<*>) {
            LiteralExpression(another.string(), line)
        } else {
            another
        }

        // At this point, `another` is a literal expression.

        // The string always dominates the expression
        if (value is String || anotherValue.value is String) {
            return LiteralExpression(string() + anotherValue.string(), line)
        }

        val leftNumber = value as Double
        val rightNumber = anotherValue.value as Double

        return LiteralExpression(leftNumber + rightNumber, line)
    }

    override fun subtract(another: ExpressionNode): ExpressionNode {
        checkIsNumber(another, BinaryOperation.SUBTRACT)

        return LiteralExpression(value as Double - (another as LiteralExpression<*>).value as Double, line)
    }

    override fun multiply(another: ExpressionNode): ExpressionNode {
        checkIsNumber(another, BinaryOperation.MULTIPLY)

        return LiteralExpression(value as Double * (another as LiteralExpression<*>).value as Double, line)
    }

    override fun divide(another: ExpressionNode): ExpressionNode {
        checkIsNumber(another, BinaryOperation.DIVIDE)

        return LiteralExpression(value as Double / (another as LiteralExpression<*>).value as Double, line)
    }

    private fun checkIsNumber(another: ExpressionNode, operator: BinaryOperation) {
        if (value is String || another !is LiteralExpression<*>) throw RuntimeException("Invalid operation candidates for operation ${operator}: ${this::class.simpleName} and ${another::class.simpleName} on line ${line}")
        if (another.value !is Double) throw RuntimeException("Invalid operation candidates for operation ${operator}: ${this::class.simpleName} and ${another::class.simpleName} on line ${line}")
    }

}

class ArrayExpression(
    val value: ArrayList<ExpressionNode>,
    line: Int,
) : ExpressionNode(line) {

    var type: Class<*>? = null

    override fun print(indent: Int) {
        println(indentation(indent) + "Array Expression:")
        value.forEachIndexed { index, expr ->
            println(indentation(indent + 1) + "${index}:")
            expr.print(indent + 2)
        }
    }

    override fun reduceToAtomic(runtime: Runtime): ArrayExpression {
        val atomicValues: ArrayList<ExpressionNode> = arrayListOf()

        value.forEachIndexed { index, node ->
            atomicValues.add(value[index].reduceToAtomic(runtime))
        }

        return ArrayExpression(atomicValues, line).validateTypes()
    }

    fun validateTypes(): ArrayExpression {
        // Check if all types in the array are equal
        val initial = value.firstOrNull()

        // We need to put SOMETHING in place of the array's type ...
        if (initial == null) {
            type = LiteralExpression::class.java
            return this
        }

        if (initial !is LiteralExpression<*>) {
            throw RuntimeException("Array expression not atomic enough on line ${line}.")
        }

        val initialType = initial.value::class.java

        value.forEachIndexed { index, it ->
            if (it !is LiteralExpression<*>) {
                throw RuntimeException("Array expression at index ${index} not atomic enough on line ${line}.")
            }

            if (it.value::class.java != initialType)
                throw RuntimeException("An array may only be made up of a single type of elements: Error on line ${line}")
        }

        type = initial::class.java

        return this
    }

    fun checkAgainst(clazz: Class<*>, length: IntRange? = null): Boolean {
        if (type != LiteralExpression::class.java)
            return false
        val initial = value.firstOrNull() ?: return length == null || length.min() == 0
        if (!((initial as LiteralExpression<*>).value)::class.java.isAssignableFrom(clazz))
            return false
        if (length != null && !length.contains(value.size)) return false
        return true
    }

}

class VariableReferenceExpression(
    val id: String,
    line: Int
) : ExpressionNode(line) {
    override fun print(indent: Int) {
        println(indentation(indent) + "Variable: ${id}")
    }

    override fun reduceToAtomic(runtime: Runtime): ExpressionNode {
        val expr = runtime.findVariable(id)
            ?: throw RuntimeException("Referenced unknown variable \"${id}\" on line ${line}.")
        return expr.reduceToAtomic(runtime)
    }
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

        return leftAtomic.operation(operator, rightAtomic)
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

class VariableAssignment(
    val id: String,
    val value: ExpressionNode,
    line: Int
) : ExpressionNode(line) {
    override fun print(indent: Int) {
        println(indentation(indent) + "Variable assignment \"${id}\":")
        value.print(indent + 1)
    }

    override fun reduceToAtomic(runtime: Runtime): ExpressionNode = value.reduceToAtomic(runtime)

}