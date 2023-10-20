package dev.vanadium.viml.parse

import dev.vanadium.viml.cmd.CommandArgument

class Script(val nodes: List<Node>)

/**
 * Superclass of all nodes ever
 */
abstract class Node

/**
 * The base class for all nodes involved in flow control and
 * commanding of the underlying graphics engine
 */
abstract class StatementNode : Node()

/**
 * Base class for all expression nodes (arrays, arithmetic ops, ...)
 */
abstract class ExpressionNode : Node()


class StringLiteralExpression (
    val value: String
) : ExpressionNode()

class IntegerLiteralExpression (
    val value: Int
) : ExpressionNode()

class ArrayExpression(
    val value: List<ExpressionNode>
) : ExpressionNode()

class InterpolationExpression(
    val from: ArrayExpression,
    val to: ArrayExpression
) : ExpressionNode() {

    init {
        require(from.value.size == to.value.size)
        require(from.value.isNotEmpty() && to.value.isNotEmpty())
        require(from.value.firstOrNull()!!.javaClass.isAssignableFrom(to.value.firstOrNull()!!.javaClass))
    }

}

class CommandNode(
    val label: String,
    val args: ArrayList<CommandArgument>
) : StatementNode()