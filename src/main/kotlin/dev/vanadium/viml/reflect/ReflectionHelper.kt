package dev.vanadium.viml.reflect

import dev.vanadium.viml.handler.CommandHandler
import org.reflections.Reflections

fun findAllClasses(): Array<Class<out Any>> {
    return Reflections("dev.vanadium.viml").getTypesAnnotatedWith(Command::class.java).toTypedArray()
}

fun findAllCommandHandlers(): HashMap<String, CommandHandler> {
    val handlers: HashMap<String, CommandHandler> = hashMapOf()

    findAllClasses().forEach {
        if (!it.interfaces.contains(CommandHandler::class.java))
            return@forEach

        val annotation: Command = it.getAnnotation(Command::class.java) ?: return@forEach

        handlers.put(annotation.label, it.getDeclaredConstructor().newInstance() as CommandHandler)
    }

    return handlers
}
