package dev.vanadium.viml.runtime

import dev.vanadium.viml.CommandNode
import dev.vanadium.viml.Node
import dev.vanadium.viml.Script
import dev.vanadium.viml.handler.CommandHandler
import dev.vanadium.viml.handler.validateArguments
import dev.vanadium.viml.reflect.findAllCommandHandlers

class Runtime(val script: Script) {

    var variables: ArrayList<Variable> = arrayListOf()
    var commandHandlers: HashMap<String, CommandHandler> = hashMapOf()

    init {
        findAllCommandHandlers().forEach { t, u ->
            registerCommand(t, u)
        }
    }

    fun findVariable(id: String): Variable {
        return variables.filter { it.id == id }.firstOrNull()
            ?: throw IllegalStateException("Trying to find an unknown variable in a runtime operation \"${id}\".")
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
    }

    private fun runCommand(node: CommandNode) {
        if (!commandHandlers.containsKey(node.label))
            throw RuntimeException("Undefined command \"${node.label}\" on line ${node.line}")

        // Make sure all command arguments are as atomic as possible
        node.args.forEach { (t, u) ->
            node.args[t] = u.reduceToAtomic(this)
        }

        val handler = commandHandlers[node.label]!!

        val validators = handler.validateArguments(node.args, this)

        if (validators != null) {
            validateArguments(node.label, node.line, node.args, validators)
        }

        handler.preflight(node.label, node.args, this, node.line)
        handler.invoke(node.label, node.args, this, node.line)
    }

}