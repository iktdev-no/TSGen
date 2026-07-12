package no.iktdev.ts

import java.io.File

object TsGenerator {
    val ttm = TsTypeMapper()
    val tmr = TsModelRenderer()

    fun generate(packageName: String, output: File, classLoader: ClassLoader = Thread.currentThread().contextClassLoader) {
        println("TsGenerator: scanning package: $packageName")

        val classes = ClassScanner().scan(packageName, classLoader)

        println("TsGenerator: found ${classes.size} classes total")
        classes.forEach { println(" - ${it.qualifiedName}") }

        val ts = buildString {
            appendLine("// AUTO-GENERATED. DO NOT EDIT.")
            appendLine("// Source: $packageName")
            appendLine()

            classes.forEach { cls ->
                when {
                    // ----------------------------------------------------
                    // 1) Sealed interface → union type alias
                    // ----------------------------------------------------
                    cls.isSealed && cls.java.isInterface -> {
                        val subTypes = cls.sealedSubclasses
                            .mapNotNull { it.simpleName }
                            .joinToString(" | ")

                        println("Generating union type for sealed interface: ${cls.simpleName} = $subTypes")
                        appendLine("export type ${cls.simpleName} = $subTypes")
                    }

                    // 1a) Sealed class → union type alias
                    cls.isSealed && !cls.java.isInterface -> {
                        val subTypes = cls.sealedSubclasses
                            .mapNotNull { it.simpleName }
                            .joinToString(" | ")

                        println("Generating union type for sealed class: ${cls.simpleName} = $subTypes")
                        appendLine("export type ${cls.simpleName} = $subTypes")
                    }


                    // ----------------------------------------------------
                    // 1b) Sealed subtype (data object / data class)
                    //     → generer interface selv uten properties
                    // ----------------------------------------------------
                    cls.isSealedSubtype() -> {
                        println("Generating sealed subtype interface: ${cls.simpleName}")
                        append(tmr.sealedSubtypeToTs(cls))
                    }

                    // ----------------------------------------------------
                    // 2) Enum → union of string literals
                    // ----------------------------------------------------
                    cls.java.isEnum -> {
                        println("Generating enum: ${cls.simpleName}")
                        append(tmr.enumToTs(cls))
                    }

                    // ----------------------------------------------------
                    // 3) Data class → interface
                    // ----------------------------------------------------
                    cls.hasProperties() && !cls.isSealed -> {
                        println("Generating interface: ${cls.simpleName}")
                        append(tmr.dataClassToTs(cls, ttm))
                    }

                    else -> {
                        println("IGNORED (no properties): ${cls.qualifiedName}")
                    }
                }
                appendLine()
            }
        }

        output.parentFile.mkdirs()
        output.writeText(ts)

        println("TsGenerator: wrote file to ${output.absolutePath}")
    }

}
