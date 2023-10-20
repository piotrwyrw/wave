package dev.vanadium.viml.parse

import dev.vanadium.viml.cmd.CommandArgument

/**
 * The base class for all nodes involved in flow control and
 * commanding of the underlying graphics engine
 */
abstract class ActionNode

/**
 * Base class for all expression nodes (arrays, arithmetic ops], ...)
 */
abstract class ExpressionNode


class LiteralExpression<T>(
    val value: T
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
) : ActionNode()