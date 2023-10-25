package dev.vanadium.wave.handler

import dev.vanadium.wave.ExpressionNode

class ArgumentValidation(val label: String, val type: ArgumentType, val clazz: Class<*>)

enum class ArgumentType {
    REQUIRED,
    OPTIONAL
}

fun isValidArgument(
    label: String,
    line: Int,
    arg: String,
    value: ExpressionNode,
    validators: Array<ArgumentValidation>
): Boolean {
    validators.forEach {
        if (it.label != arg)
            return@forEach

        val type = it.clazz

        if (type == Any::class.java)
            return@forEach

        if (!value::class.java.isAssignableFrom(type))
            throw RuntimeException("The argument \"${arg}\" of command \"${label}\" is expected to be of type ${type.simpleName}, got ${value::class.simpleName} on line ${line}")

        return true
    }

    return false
}

fun validateArguments(
    label: String,
    line: Int,
    args: HashMap<String, ExpressionNode>,
    validators: Array<ArgumentValidation>
) {
    // First, check if all present arguments are in fact correct and expected
    args.forEach {
        if (!isValidArgument(label, line, it.key, it.value, validators))
            throw RuntimeException("Unexpected argument \"${it.key}\" for command \"${label}\" on line ${line}")

    }

    // Check if any required arguments are missing
    validators.forEach {
        if (args[it.label] == null && it.type == ArgumentType.REQUIRED)
            throw RuntimeException("Required argument \"${it.label}\" not provided for command ${label} on line ${line}")
    }
}