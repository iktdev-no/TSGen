package no.iktdev.ts

import java.io.File

object TsGenerator {
    var versionInfo: String = System.getProperty("tsgenerator.version")
        ?: TsGenerator::class.java.getPackage()?.implementationVersion
        ?: "dev"
    var buildTime: String = java.time.Instant.now().toString()

    val ttm = TsTypeMapper()
    val tmr = TsModelRenderer()

    fun generate(
        packageName: String,
        output: File,
        classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
        includeTypedSealed: Boolean = true,
        includeTypedInterface: Boolean = true
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
                    // 1) Sealed interface → union type alias
                    // ----------------------------------------------------
                    cls.isSealed && cls.java.isInterface -> {
                        println("Generating union type for sealed interface: ${cls.simpleName}")
                        append(tmr.sealedUnionToTs(cls))
                    }

                    // ----------------------------------------------------
                    // 1a) Sealed class → union type alias
                    // ----------------------------------------------------
                    cls.isSealed && !cls.java.isInterface -> {
                        println("Generating union type for sealed class: ${cls.simpleName}")
                        append(tmr.sealedUnionToTs(cls))
                    }

                    // ----------------------------------------------------
                    // 1b) Sealed subtype (data object / data class)
                    //     → Bruker includeTypedSealed for type-lappen
                    // ----------------------------------------------------
                    cls.isSealedSubtype() -> {
                        println("Generating sealed subtype interface: ${cls.simpleName}")
                        append(tmr.dataClassToTs(cls, ttm, includeTypedSealed))
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
                    //     → Bruker includeTypedInterface for type-lappen
                    // ----------------------------------------------------
                    cls.hasProperties() && !cls.isSealed -> {
                        println("Generating interface: ${cls.simpleName}")
                        append(tmr.dataClassToTs(cls, ttm, includeTypedInterface))
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