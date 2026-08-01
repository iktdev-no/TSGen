package no.iktdev.ts

import java.io.File

object TsGenerator {
    var versionInfo: String = System.getProperty("tsgenerator.version")
        ?: TsGenerator::class.java.getPackage()?.implementationVersion
        ?: "any"
    var buildTime: String = java.time.Instant.now().toString()

    val tmr = TsModelRenderer()

    fun generate(
        packageName: String,
        output: File,
        classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
        writeSealedStrategy: SealedStrategy,
    ) {
        println("TsGenerator: scanning package: $packageName")

        val classes = ClassScanner().scan(packageName, classLoader)

        println("TsGenerator: found ${classes.size} classes total")
        classes.forEach { println(" - ${it.qualifiedName}") }

        val ts = buildString {
            appendLine("// AUTO-GENERATED. DO NOT EDIT.")
            appendLine("// TSGenerator Version: $versionInfo")
            appendLine("// Time: $buildTime")
            appendLine("// Source: $packageName")
            appendLine()

            classes.forEach { cls ->
                when {
                    // ----------------------------------------------------
                    // 1) Sealed interface → union type alias + evt. base interface
                    // ----------------------------------------------------
                    cls.isSealed && cls.java.isInterface -> {
                        println("Generating sealed interface: ${cls.simpleName}")
                        append(tmr.sealedUnionToTs(cls, writeSealedStrategy))
                    }

                    // ----------------------------------------------------
                    // 1a) Sealed class → union type alias + evt. base interface
                    // ----------------------------------------------------
                    cls.isSealed && !cls.java.isInterface -> {
                        println("Generating sealed class: ${cls.simpleName}")
                        append(tmr.sealedUnionToTs(cls, writeSealedStrategy))
                    }

                    // ----------------------------------------------------
                    // 1b) Sealed subtype (data object / data class)
                    //     Sjekker strategien for type-felt og extends-base
                    // ----------------------------------------------------
                    cls.isSealedSubtype() -> {
                        println("Generating sealed subtype interface: ${cls.simpleName}")

                        // Finn baseklassen/interfacet hvis strategien tillater det
                        val baseName = if (writeSealedStrategy != SealedStrategy.ONLY_TYPED) {
                            cls.java.superclass?.kotlin?.takeIf { it.isSealed }?.simpleName
                                ?: cls.java.interfaces.map { it.kotlin }.firstOrNull { it.isSealed }?.simpleName
                        } else {
                            null
                        }

                        append(tmr.sealedSubtypeToTs(cls, writeSealedStrategy, baseName))
                    }

                    // ----------------------------------------------------
                    // 2) Enum → union of string literals
                    // ----------------------------------------------------
                    cls.java.isEnum -> {
                        println("Generating enum: ${cls.simpleName}")
                        append(tmr.enumToTs(cls))
                    }

                    // ----------------------------------------------------
                    // 3) Data class → interface (vanlige klasser)
                    // ----------------------------------------------------
                    cls.hasProperties() && !cls.isSealed -> {
                        println("Generating interface: ${cls.simpleName}")
                        append(tmr.dataClassToTs(cls))
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