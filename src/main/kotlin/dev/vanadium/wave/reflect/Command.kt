package dev.vanadium.wave.reflect

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Command(val label: String)
