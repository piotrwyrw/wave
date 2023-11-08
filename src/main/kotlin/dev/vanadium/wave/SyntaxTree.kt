package dev.vanadium.wave

import dev.vanadium.wave.analysis.lexical.Token
import dev.vanadium.wave.analysis.lexical.TokenType
import dev.vanadium.wave.analysis.lexical.compareToken
import dev.vanadium.wave.exception.SyntaxError
import dev.vanadium.wave.runtime.Runtime
import kotlin.math.abs
import kotlin.math.pow

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
abstract class Node(val line: Int, val superBlock: BlockNode?) {
    abstract fun print(indent: Int)

    protected fun indentation(indent: Int): String {
        return " ".repeat(indent * 4)
    }

}

/**
 * The base class for all nodes involved in flow control and
 * commanding of the underlying graphics engine
 */
abstract class StatementNode(line: Int, superBlock: BlockNode?) : Node(line, superBlock)

/**
 * Base class for all expression nodes (arrays, arithmetic ops, ...)
 */
abstract class ExpressionNode(line: Int, superBlock: BlockNode?) : Node(line, superBlock) {

    /**
     * Reduces the expression to a form that is (hopefully) more atomic
     */
    open fun reduce(runtime: Runtime): ExpressionNode {
        if (javaClass.getAnnotation(RuntimeOutsourcedReduction::class.java) != null)
            return runtime.processExpression(this).second

        return this
    }

    // TODO: White a better implementation for this function which doesn't guess the number of required reduction
    fun atomic(runtime: Runtime): ExpressionNode {
        return reduce(runtime).reduce(runtime)
    }

    fun binaryOperation(op: BinaryOperation, another: ExpressionNode): ExpressionNode {
        val allOps = allOperations(another, op)
        if (allOps != null) {
            return allOps
        }

        return when (op) {
            BinaryOperation.ADD -> add(another)
            BinaryOperation.SUBTRACT -> subtract(another)
            BinaryOperation.MULTIPLY -> multiply(another)
            BinaryOperation.DIVIDE -> divide(another)
            BinaryOperation.POWER -> power(another)
            BinaryOperation.EVALUATE_AT -> evaluateAt(another)
        }
    }

    fun unaryOperation(op: UnaryOperation): ExpressionNode {
        return when (op) {
            UnaryOperation.MAGNITUDE -> magnitude()
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

    open fun power(another: ExpressionNode): ExpressionNode =
        throw RuntimeException("Illegal power operation on ${this::class.java.simpleName} on line ${line}")

    open fun magnitude(): ExpressionNode =
        throw RuntimeException("Illegal magnitude operation on ${this::class.java.simpleName} on line ${line}")

    open fun evaluateAt(at: ExpressionNode): ExpressionNode =
        throw RuntimeException("Illegal evaluate at operation on ${this::class.java.simpleName} on line ${line}")

    open fun string(): String =
        throw RuntimeException("The object \"${this::class.java.simpleName}\" does not support string conversions. Attempted on line ${line}")

}


class LiteralExpression<T : Any>(
    val value: T,
    line: Int,
    superBlock: BlockNode?
) : ExpressionNode(line, superBlock) {

    companion object {
        fun zero(line: Int, sBlock: BlockNode): LiteralExpression<Double> {
            return LiteralExpression(0.0, line, sBlock)
        }
    }

    init {
        if (value !is Double && value !is String)
            throw RuntimeException("A literal expression is expected to be either Double or String, gpt ${value::class.simpleName} on line ${line}")
    }

    override fun print(indent: Int) {
        println(indentation(indent) + "Literal \"${value}\"")
    }

    override fun allOperations(another: ExpressionNode, operator: BinaryOperation): ExpressionNode? {
        if (another !is LiteralExpression<*> && value !is String) throw RuntimeException("Cannot perform a binary operation on a literal and a complex on line ${line}")
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
            LiteralExpression(another.string(), line, superBlock)
        } else {
            another
        }

        // At this point, `another` is a literal expression.

        // The string always dominates the expression
        if (value is String || anotherValue.value is String) {
            return LiteralExpression(string() + anotherValue.string(), line, superBlock)
        }

        val leftNumber = value as Double
        val rightNumber = anotherValue.value as Double

        return LiteralExpression(leftNumber + rightNumber, line, superBlock)
    }

    override fun subtract(another: ExpressionNode): ExpressionNode {
        checkIsNumber(another, BinaryOperation.SUBTRACT)

        return LiteralExpression(value as Double - (another as LiteralExpression<*>).value as Double, line, superBlock)
    }

    override fun multiply(another: ExpressionNode): ExpressionNode {
        checkIsNumber(another, BinaryOperation.MULTIPLY)

        return LiteralExpression(value as Double * (another as LiteralExpression<*>).value as Double, line, superBlock)
    }

    override fun divide(another: ExpressionNode): ExpressionNode {
        checkIsNumber(another, BinaryOperation.DIVIDE)

        return LiteralExpression(value as Double / (another as LiteralExpression<*>).value as Double, line, superBlock)
    }

    override fun power(another: ExpressionNode): ExpressionNode {
        checkIsNumber(another, BinaryOperation.POWER)

        return PowerExpression(this, another, line, superBlock)
    }

    override fun magnitude(): ExpressionNode {
        return when (value::class) {
            String::class -> LiteralExpression((value as String).length, line, superBlock)
            Double::class -> LiteralExpression(abs(value as Double), line, superBlock)
            else -> throw RuntimeException("Invalid type for literal value encountered on line ${line}: ${value::class.simpleName}")
        }
    }

    private fun checkIsNumber(another: ExpressionNode, operator: BinaryOperation) {
        if (value is String || another !is LiteralExpression<*>) throw RuntimeException("Invalid operation candidates for operation ${operator}: ${this::class.simpleName} and ${another::class.simpleName} on line ${line}")
        if (another.value !is Double) throw RuntimeException("Invalid operation candidates for operation ${operator}: ${this.value::class.simpleName} and ${another.value::class.simpleName} on line ${line}")
    }

}

class ArrayExpression(
    val value: ArrayList<ExpressionNode>,
    line: Int,
    superBlock: BlockNode?
) : ExpressionNode(line, superBlock) {

    var type: Class<*>? = null

    override fun print(indent: Int) {
        println(indentation(indent) + "Array Expression:")
        value.forEachIndexed { index, expr ->
            println(indentation(indent + 1) + "${index}:")
            expr.print(indent + 2)
        }
    }

    override fun add(another: ExpressionNode): ExpressionNode {
        if (another !is ArrayExpression)
            throw RuntimeException("Cannot add an array to ${another::class.simpleName} on line ${line}")

        if (value.size != another.value.size)
            throw RuntimeException("Both arrays have to be of the same size during an addition operation. Error on line ${line}")

        val expressions: ArrayList<ExpressionNode> = arrayListOf()

        value.forEachIndexed { index, expressionNode ->
            expressions.add(BinaryExpressionNode(expressionNode, another.value[index], BinaryOperation.ADD, line, superBlock))
        }

        return ArrayExpression(expressions, line, superBlock)
    }

    override fun subtract(another: ExpressionNode): ExpressionNode {
        if (another !is ArrayExpression)
            throw RuntimeException("Cannot subtract ${another::class.simpleName} from an array on line ${line}")

        if (value.size != another.value.size)
            throw RuntimeException("Both arrays have to be of the same size during a subtraction operation. Error on line ${line}")

        val expressions: ArrayList<ExpressionNode> = arrayListOf()

        value.forEachIndexed { index, expressionNode ->
            expressions.add(BinaryExpressionNode(expressionNode, another.value[index], BinaryOperation.SUBTRACT, line, superBlock))
        }

        return ArrayExpression(expressions, line, superBlock)
    }

    override fun magnitude(): ExpressionNode {
        if (value.size == 0)
            return LiteralExpression(0.0, line, superBlock)

        var left: ExpressionNode = PowerExpression(value.first(), LiteralExpression(2.0, line, superBlock), line, superBlock)

        value.forEachIndexed { index, expressionNode ->
            if (index == 0)
                return@forEachIndexed

            left = BinaryExpressionNode(
                left,
                PowerExpression(expressionNode, LiteralExpression(2.0, line, superBlock), line, superBlock),
                BinaryOperation.ADD,
                line,
                superBlock
            )
        }

        return PowerExpression(left, LiteralExpression(1.0 / 2.0, line, superBlock), line, superBlock)
    }

    override fun evaluateAt(at: ExpressionNode): ExpressionNode {
        if (at !is LiteralExpression<*>)
            throw RuntimeException("Evaluation point is not atomic enough on line ${line}.")

        if (at.value !is Double)
            throw RuntimeException("Evaluation point is supposed to be a number, got ${at.value::class.simpleName} on line ${line}.")

        if (at.value.toInt() !in 0..<value.size)
            throw RuntimeException("Cannot access item on index ${at.value.toInt()} of array of length ${value.size}.")

        return value.get(at.value.toInt())
    }

    override fun reduce(runtime: Runtime): ArrayExpression {
        val atomicValues: ArrayList<ExpressionNode> = arrayListOf()

        value.forEachIndexed { index, node ->
            atomicValues.add(value[index].atomic(runtime))
        }

        return ArrayExpression(atomicValues, line, superBlock).validateTypes()
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
                throw RuntimeException("An array may only be made up of a single type of element: Error on line ${line}")
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
    line: Int,
    superBlock: BlockNode?
) : ExpressionNode(line, superBlock) {
    override fun print(indent: Int) {
        println(indentation(indent) + "Variable: ${id}")
    }

    override fun reduce(runtime: Runtime): ExpressionNode {
        val expr = runtime.findVariable(id)
            ?: throw RuntimeException("Referenced unknown variable \"${id}\" on line ${line}.")
        return expr.atomic(runtime)
    }
}

class InterpolationExpression(
    val from: ExpressionNode,
    val to: ExpressionNode,
    line: Int,
    superBlock: BlockNode?
) : ExpressionNode(line, superBlock) {
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
    DIVIDE,
    POWER,
    EVALUATE_AT;
}

fun binaryOperationFromToken(token: Token): BinaryOperation {
    return when (token.type) {
        TokenType.PLUS -> BinaryOperation.ADD
        TokenType.SLASH -> BinaryOperation.DIVIDE
        TokenType.MINUS -> BinaryOperation.SUBTRACT
        TokenType.ASTERISK -> BinaryOperation.MULTIPLY
        TokenType.HAT -> BinaryOperation.POWER
        TokenType.AT -> BinaryOperation.EVALUATE_AT
        else -> throw SyntaxError("Unknown binary operator ${token.type} on line ${token.line}")
    }
}

class BinaryExpressionNode(
    val left: ExpressionNode,
    val right: ExpressionNode,
    val operator: BinaryOperation,
    line: Int,
    superBlock: BlockNode?
) : ExpressionNode(line, superBlock) {
    override fun print(indent: Int) {
        println(indentation(indent) + "Binary operation (${operator}):")
        println(indentation(indent + 1) + "Left:")
        left.print(indent + 2)

        println(indentation(indent + 1) + "Right")
        right.print(indent + 2)
    }

    override fun reduce(runtime: Runtime): ExpressionNode {
        val leftAtomic = left.atomic(runtime)
        val rightAtomic = right.atomic(runtime)

        return leftAtomic.binaryOperation(operator, rightAtomic).atomic(runtime)
    }
}

enum class UnaryOperation {
    MAGNITUDE
}

fun unaryOperationFromToken(token: Token): UnaryOperation {
    return when (token.type) {
        TokenType.VBAR -> UnaryOperation.MAGNITUDE
        else -> throw SyntaxError("Unknown unary operator ${token.type} on line ${token.line}")
    }
}

class UnaryOperationNode(
    val expression: ExpressionNode,
    val operator: UnaryOperation,
    line: Int,
    superBlock: BlockNode?
) : ExpressionNode(line, superBlock) {
    override fun print(indent: Int) {
        println(indentation(indent) + "Unary operation (${operator}):")
        expression.print(indent + 1)
    }

    override fun reduce(runtime: Runtime): ExpressionNode {
        return expression.atomic(runtime).unaryOperation(operator).atomic(runtime)
    }

}

class CommandNode(
    val label: String,
    val args: HashMap<String, ExpressionNode>,
    line: Int,
    superBlock: BlockNode?
) : StatementNode(line, superBlock) {
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

enum class VariableAssignmentType {
    DECLARATION,
    MUTATION
}

class VariableOperation(
    val id: String,
    val value: ExpressionNode,
    val instant: Boolean = false,
    val type: VariableAssignmentType,
    line: Int,
    superBlock: BlockNode?
) : ExpressionNode(line, superBlock) {
    override fun print(indent: Int) {
        println(indentation(indent) + "Variable operation ${type} \"${id}\":")
        value.print(indent + 1)
    }

    override fun reduce(runtime: Runtime): ExpressionNode = value.atomic(runtime)

}

class PowerExpression(
    val expression: ExpressionNode,
    val power: ExpressionNode,
    line: Int,
    superBlock: BlockNode?
) : ExpressionNode(line, superBlock) {
    override fun print(indent: Int) {
        println(indentation(indent) + "Exponential expression (Power ${power}):")
        expression.print(indent + 1)
    }

    override fun reduce(runtime: Runtime): ExpressionNode {
        val atomic = expression.atomic(runtime)
        if (atomic !is LiteralExpression<*>)
            throw RuntimeException("Exponential expression not atomic enough on line ${line}.")
        if (atomic.value !is Double)
            throw RuntimeException("Exponential expression must be a number. Error on line ${line}.")
        val power: ExpressionNode = power.atomic(runtime)
        if (power !is LiteralExpression<*>)
            throw RuntimeException("Exponential degree not atomic enough")
        if (power.value !is Double)
            throw RuntimeException("Exponential degree must be a number.")
        return LiteralExpression(atomic.value.pow(power.value), line, superBlock)
    }
}

class RepeatNode(
    val count: ExpressionNode,
    val block: BlockNode,
    val variable: Token,
    line: Int,
    superBlock: BlockNode?
) : ExpressionNode(line, superBlock) {
    init {
        if (variable.type != TokenType.IDENTIFIER)
            throw SyntaxError("Target variable name is expected to be an identifier, got ${variable.type} in RepeatNode on line ${line}")
    }

    override fun print(indent: Int) {
        println(indentation(indent) + "Repeat (${variable}):")
        println(indentation(indent + 1) + "Count:")
        count.print(indent + 2);

        block.print(indent + 1)
    }

}

@RuntimeOutsourcedReduction
class BlockNode(
    val nodes: List<Node>,
    line: Int,
    superBlock: BlockNode?
) : ExpressionNode(line, superBlock) {

    var holder: Node? = null
    var variables: HashMap<String, ExpressionNode> = hashMapOf()

    override fun print(indent: Int) {
        println(indentation(indent) + "Block:")
        if (holder != null)
            println(indentation(indent + 1) + "Held by: ${holder!!::class.simpleName} on line ${holder!!.line}")
        else
            println(indentation(indent + 1) + "No holder.")
        nodes.forEach {
            it.print(indent + 1)
        }
    }

    fun withHolder(holder: Node?): BlockNode {
        this.holder = holder
        return this
    }

}

class FunctionDefinitionNode(
    val name: Token,
    val block: BlockNode,
    line: Int,
    superBlock: BlockNode?
) : StatementNode(line, superBlock) {
    override fun print(indent: Int) {
        println(indentation(indent) + "Function definition (${name.value}):")
        block.print(indent + 1)
    }

}

class FunctionCall(
    val name: Token,
    line: Int,
    superBlock: BlockNode?
) : ExpressionNode(line, superBlock) {
    init {
        if (!compareToken(name, TokenType.IDENTIFIER))
            throw SyntaxError("Function name is expected to be an identifier, got ${name.type} on line ${line}")
    }

    override fun print(indent: Int) {
        println(indentation(indent) + "Function call (${name.value})")
    }
}

class ReturnNode(
    val expression: ExpressionNode,
    line: Int,
    superBlock: BlockNode?
) : StatementNode(line, superBlock) {
    override fun print(indent: Int) {
        println(indentation(indent) + "Return:")
        expression.print(indent + 1)
    }
}