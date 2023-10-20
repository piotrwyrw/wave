package dev.vanadium.viml.cmd

interface CommandHandler {

    fun command(label: String, args: ArrayList<CommandArgument>)

}