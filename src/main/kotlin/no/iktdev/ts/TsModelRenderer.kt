package no.iktdev.ts

import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

class TsModelRenderer {

    fun enumToTs(cls: KClass<*>): String {
        val values = cls.java.enumConstants
            .joinToString(" | ") { "\"$it\"" }

        return "export type ${cls.simpleName} = $values\n"
    }

    fun sealedSubtypeToTs(cls: KClass<*>, ttm: TsTypeMapper, includeTypeDiscriminator: Boolean): String {
        return dataClassToTs(cls, ttm, includeTypeDiscriminator)
    }

    fun dataClassToTs(cls: KClass<*>, ttm: TsTypeMapper, includeTypeDiscriminator: Boolean): String {
        val typeParams = cls.typeParameters.map { it.name }
        val generic = if (typeParams.isNotEmpty()) "<" + typeParams.joinToString(", ") + ">" else ""

        val superClasses = mutableListOf<KClass<*>>()
        val superClass = cls.java.superclass?.kotlin
        if (superClass != null && superClass != Any::class) {
            superClasses.add(superClass)
        }

        val superInterfaces = cls.java.interfaces
            .map { it.kotlin }
            .filter { it.simpleName != "Any" && it.simpleName != "Serializable" }

        superClasses.addAll(superInterfaces)

        val extendsClause = if (superClasses.isNotEmpty()) {
            " extends ${superClasses.joinToString(", ") { it.simpleName!! }}"
        } else ""

        val inheritedPropNames = superClasses.flatMap { it.memberProperties }.map { it.name }.toSet()

        // Sjekk om klassen har et 'type'-felt, og tving det til klassens navn hvis flagget er satt
        val hasTypeProp = cls.memberProperties.any { it.name == "type" }

        val props = cls.memberProperties
            .filter { it.name !in inheritedPropNames }
            .joinToString("\n") { prop ->
                val tsType = if (prop.name == "type" && includeTypeDiscriminator) {
                    // Tving type-feltet til å bli en string literal basert på klassens navn (f.eks. "FileCopyProgress")
                    "\"${cls.simpleName}\""
                } else {
                    ttm.kotlinToTsType(prop.returnType.toString(), typeParams)
                }
                "  ${prop.name}: $tsType;"
            }

        return buildString {
            appendLine("export interface ${cls.simpleName}$generic$extendsClause {")
            // Hvis klassen ikke har 'type'-felt i det hele tatt, men skal ha diskriminator, legg til den
            if (includeTypeDiscriminator && !hasTypeProp && "type" !in inheritedPropNames) {
                appendLine("  type: \"${cls.simpleName}\";")
            }
            if (props.isNotBlank()) appendLine(props)
            appendLine("}")
        }
    }
}