package dev.vanadium.viml.exception

class SyntaxError(var msg: String) : RuntimeException(msg)