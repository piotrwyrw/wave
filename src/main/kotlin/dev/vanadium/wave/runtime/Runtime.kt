package dev.vanadium.wave.runtime

import dev.vanadium.wave.*
import dev.vanadium.wave.handler.CommandHandler
import dev.vanadium.wave.handler.validateArguments
import dev.vanadium.wave.reflect.findAllCommandHandlers

class Runtime(val script: Script) {

    var variables: HashMap<String, ExpressionNode> = hashMapOf()
    var commandHandlers: HashMap<String, CommandHandler> = hashMapOf()

    init {
        findAllCommandHandlers().forEach { (t, u) ->
            registerCommand(t, u)
        }
    }

    fun findVariable(id: String): ExpressionNode? {
        return variables[id]
    }

    fun registerCommand(label: String, handler: CommandHandler) {
        if (commandHandlers.containsKey(label))
            throw RuntimeException("Command \"${label}\" is already registered. Cannot overwrite.")

        commandHandlers[label] = handler
    }

    fun run() {
        for (node in script.nodes)
            runNode(node)
    }

    private fun runNode(node: Node) {
        if (node is CommandNode)
            runCommand(node)

        if (node is VariableAssignment)
            assignVariable(node)

        if (node is RepeatNode)
            runRepeatStatement(node)
    }

    private fun runRepeatStatement(repeat: RepeatNode) {
        val count = repeat.count.reduceToAtomic(this).reduceToAtomic(this)

        if (count !is LiteralExpression<*>)
            throw RuntimeException("Expected literal expression for target value in repeat statement on line ${repeat.line}, got ${count::class.simpleName}")

        if (count.value !is Double)
            throw RuntimeException("The target value in repeat statement is meant to be a number, got ${count.value::class.simpleName}")

        repeat(count.value.toInt()) {
            assignVariable(VariableAssignment(repeat.variable.value, LiteralExpression<Double>(it.toDouble(), repeat.line), false, repeat.line))
            repeat.blocK.nodes.forEach {
                runNode(it)
            }
        }
    }

    private fun assignVariable(assignment: VariableAssignment) {
        variables[assignment.id] =
            if (!assignment.invariable) assignment.value else assignment.value.reduceToAtomic(this).reduceToAtomic(this)
    }

    private fun runCommand(node: CommandNode) {
        if (!commandHandlers.containsKey(node.label))
            throw RuntimeException("Undefined command \"${node.label}\" on line ${node.line}")

        var atomicArgs: HashMap<String, ExpressionNode> = hashMapOf()

        // Make sure all command arguments are as atomic as possible
        node.args.forEach { (t, u) ->
            val atomic = u.reduceToAtomic(this)
            atomicArgs[t] = atomic
        }

        val handler = commandHandlers[node.label]!!

        val validators = handler.validateArguments(atomicArgs, this)

        if (validators != null) {
            validateArguments(node.label, node.line, atomicArgs, validators)
        }

        handler.preflight(atomicArgs, this, node.line)
        handler.invoke(atomicArgs, this, node.line)
    }

}