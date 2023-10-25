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
    }

    private fun assignVariable(assignment: VariableAssignment) {
        variables[assignment.id] = assignment.value
    }

    private fun runCommand(node: CommandNode) {
        if (!commandHandlers.containsKey(node.label))
            throw RuntimeException("Undefined command \"${node.label}\" on line ${node.line}")

        // Make sure all command arguments are as atomic as possible
        node.args.forEach { (t, u) ->
            val atomic = u.reduceToAtomic(this)
            node.args[t] = atomic
        }

        val handler = commandHandlers[node.label]!!

        val validators = handler.validateArguments(node.args, this)

        if (validators != null) {
            validateArguments(node.label, node.line, node.args, validators)
        }

        handler.preflight(node.args, this, node.line)
        handler.invoke(node.args, this, node.line)
    }

}