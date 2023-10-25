package dev.vanadium.wave.exception

class SyntaxError(var msg: String) : RuntimeException(msg)