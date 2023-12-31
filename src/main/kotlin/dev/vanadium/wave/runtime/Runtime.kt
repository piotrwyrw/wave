package dev.vanadium.wave.runtime

import dev.vanadium.wave.*
import dev.vanadium.wave.handler.CommandHandler
import dev.vanadium.wave.handler.validateArguments
import dev.vanadium.wave.reflect.findAllCommandHandlers

class Runtime(val script: Script) {

    var variables: HashMap<String, ExpressionNode> = hashMapOf()
    var functions: HashMap<String, FunctionDefinitionNode> = hashMapOf()
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

    private fun runNode(node: Node): Pair<YieldType, ExpressionNode>? {
        if (node is ExpressionNode) {
            return processExpression(node)
        }

        if (node is CommandNode) {
            runCommand(node)
            return null
        }

        if (node is ReturnNode) {
            return Pair(YieldType.RETURN, processExpression(node.expression).second)
        }

        if (node is FunctionDefinitionNode) {
            defineFunction(node)
            return null
        }

        return null
    }

    fun processExpression(expr: ExpressionNode): Pair<YieldType, ExpressionNode> {
        if (expr is FunctionCall) {
            val fn: FunctionDefinitionNode = functions[expr.name.value]
                ?: throw RuntimeException("Attempted call to an undefined function '${expr.name.value}' on line ${expr.line}")

            val e = processExpression(fn.block)

            return e
        }

        if (expr is BlockNode) {
            return runBlock(expr) ?: Pair(YieldType.EXPRESSION, LiteralExpression(0.0, expr.line, expr.superBlock))
        }

        if (expr is ArrayExpression) {
            return Pair(YieldType.EXPRESSION, expr.atomic(this))
        }

        if (expr is VariableOperation) {
            assignVariable(expr)
            return Pair(YieldType.EXPRESSION, expr)
        }

        if (expr is RepeatNode) {
            return runRepeatStatement(expr) ?: Pair(
                YieldType.EXPRESSION,
                LiteralExpression(0.0, expr.line, expr.superBlock)
            )
        }

        return Pair(YieldType.EXPRESSION, expr)
    }

    private fun defineFunction(node: FunctionDefinitionNode) {
        if (functions[node.name.value] != null)
            throw RuntimeException("Attempted redefinition of function '${node.name.value}' on line ${node.line}. Original definition on line ${functions[node.name.value]!!.line}")

        functions[node.name.value] = node
    }

    private fun runRepeatStatement(repeat: RepeatNode): Pair<YieldType, ExpressionNode>? {
        val count = repeat.count.atomic(this)

        if (count !is LiteralExpression<*>)
            throw RuntimeException("Expected literal expression for target value in repeat statement on line ${repeat.line}, got ${count::class.simpleName}")

        if (count.value !is Double)
            throw RuntimeException("The target value in repeat statement is meant to be a number, got ${count.value::class.simpleName}")

        // Declare the variable before using it in the repeat block
        // And yes, we're intentionally using repeat's block instead of it's superBlock
        // [after all, we want to declare the variable inside of the rep. block]
        if (variables[repeat.variable.value] == null) {
            assignVariable(
                VariableOperation(
                    repeat.variable.value,
                    LiteralExpression(0.0, repeat.line, repeat.block),
                    false,
                    VariableAssignmentType.DECLARATION,
                    repeat.line, repeat.block
                )
            );
        }

        repeat(count.value.toInt()) {
            assignVariable(
                VariableOperation(
                    repeat.variable.value,
                    LiteralExpression(it.toDouble(), repeat.line, repeat.block),
                    false,
                    VariableAssignmentType.MUTATION,
                    repeat.line,
                    repeat.block
                )
            )
            val e = runBlock(repeat.block) ?: return@repeat
        }

        return null
    }

    private fun runBlock(block: BlockNode): Pair<YieldType, ExpressionNode>? {
        block.nodes.forEach {
            val retVal = runNode(it) ?: return@forEach

            if (retVal.first != YieldType.RETURN)
                return@forEach

            return retVal
        }

        if (block.holder == null)
            throw RuntimeException("All blocks used in an expression context must return a value. Consider using a block statement on line ${block.line}")

        return null
    }

    private fun assignVariable(assignment: VariableOperation) {
        if (assignment.type == VariableAssignmentType.DECLARATION && variables[assignment.id] != null) {
            throw RuntimeException("Attempted redefinition of variable \"${assignment.id}\" on line ${assignment.line}. Previous definition on line ${variables[assignment.id]!!.line}")
        }

        if (assignment.type == VariableAssignmentType.MUTATION && variables[assignment.id] == null) {
            throw RuntimeException("Attempting to mutate an undefined variable \"${assignment.id}\" on line ${assignment.line}")
        }

        // TODO: Detect self-references and enforce the instant modifier!

        val atomicValue = assignment.value.atomic(this)

        val expr = processExpression(
            if (!assignment.instant) assignment.value else atomicValue
        )

        // Non-instant array expressions are not allowed, as they would be very inefficient to test against potential circular
        // dependencies and are hence more prone to causing stack overflows and infinite recursion issues.
        if (expr::class == ArrayExpression::class && !assignment.instant) {
            throw RuntimeException("The array-valued variable \"${assignment.id}\" on line ${assignment.line} must be marked \"instant\" for stability reasons.")
        }

        variables[assignment.id] = expr.second
    }

    private fun runCommand(node: CommandNode) {
        if (!commandHandlers.containsKey(node.label))
            throw RuntimeException("Undefined command \"${node.label}\" on line ${node.line}")

        val atomicArgs: HashMap<String, ExpressionNode> = hashMapOf()

        // Make sure all command arguments are as atomic as possible
        node.args.forEach { (t, u) ->
            val atomic = u.atomic(this)
            atomicArgs[t] = processExpression(atomic).second.atomic(this)
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