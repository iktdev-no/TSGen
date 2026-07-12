package no.iktdev.ts

import no.iktdev.ts.TsGenerator.ttm
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

class TsModelRenderer {
    fun enumToTs(cls: KClass<*>): String {
        val values = cls.java.enumConstants
            .joinToString(" | ") { "\"$it\"" }

        return "export type ${cls.simpleName} = $values\n"
    }

    fun sealedSubtypeToTs(cls: KClass<*>): String {
        val props = cls.memberProperties
            .filter { it.name != "type" } // ← FIX: unngå duplisering
            .joinToString("\n") { prop ->
                val tsType = ttm.kotlinToTsType(prop.returnType.toString())
                "  ${prop.name}: $tsType;"
            }

        val propsBlock = if (props.isBlank()) "" else "\n$props"

        return buildString {
            appendLine("export interface ${cls.simpleName} {")
            appendLine("  type: \"${cls.simpleName}\";$propsBlock")
            appendLine("}")
        }
    }


    // NEW: extract default value for "type" discriminator
    fun getDefaultTypeLiteral(cls: KClass<*>): String? {
        val ctor = cls.primaryConstructor ?: return null
        val param = ctor.parameters.find { it.name == "type" } ?: return null

        return try {
            val instance = ctor.callBy(emptyMap())
            val prop = cls.memberProperties.find { it.name == "type" }
            prop?.getter?.call(instance)?.toString()
        } catch (_: Throwable) {
            null
        }
    }

    fun dataClassToTs(cls: KClass<*>, ttm: TsTypeMapper): String {
        val typeParams = cls.typeParameters.map { it.name }
        val generic = if (typeParams.isNotEmpty()) "<" + typeParams.joinToString(", ") + ">" else ""

        // 1. Bruk java.interfaces for å sikre at vi kun får reelle interfaces du selv har definert
        // Filtrer bort tekniske ting som Serializable
        val superInterfaces = cls.java.interfaces
            .map { it.kotlin }
            .filter { it.simpleName != "Any" && it.simpleName != "Serializable" }

        // Debug: Se hva vi faktisk finner
        // println("DEBUG: ${cls.simpleName} extends ${superInterfaces.map { it.simpleName }}")

        // 2. Lag "extends"-biten
        val extendsClause = if (superInterfaces.isNotEmpty()) {
            " extends ${superInterfaces.joinToString(", ") { it.simpleName!! }}"
        } else ""

        // 3. Finn egenskaper som finnes i super-interfacer
        val inheritedPropNames = superInterfaces.flatMap { it.memberProperties }.map { it.name }.toSet()

        val defaultTypeLiteral = getDefaultTypeLiteral(cls)

        // 4. Generer kun props som IKKE er arvet
        val props = cls.memberProperties
            .filter { it.name !in inheritedPropNames }
            .joinToString("\n") { prop ->
                val tsType = if (prop.name == "type" && defaultTypeLiteral != null) {
                    "\"$defaultTypeLiteral\""
                } else {
                    ttm.kotlinToTsType(prop.returnType.toString(), typeParams)
                }
                "  ${prop.name}: $tsType;"
            }

        return buildString {
            appendLine("export interface ${cls.simpleName}$generic$extendsClause {")
            if (props.isNotBlank()) appendLine(props)
            appendLine("}")
        }
    }
}