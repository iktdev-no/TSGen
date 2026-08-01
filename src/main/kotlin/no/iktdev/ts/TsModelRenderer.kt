package no.iktdev.ts

import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

class TsModelRenderer {

    fun enumToTs(cls: KClass<*>): String {
        val values = cls.java.enumConstants
            .joinToString(" | ") { "\"$it\"" }

        return "export type ${cls.simpleName} = $values\n"
    }

    // Genererer union-typen for sealed klasser (f.eks. export type Progress = A | B | C)
    fun sealedUnionToTs(cls: KClass<*>): String {
        val subTypes = cls.sealedSubclasses
            .mapNotNull { it.simpleName }
            .joinToString(" | ")

        return "export type ${cls.simpleName} = $subTypes\n"
    }

    // Genererer standard interface for klasser/subtyper
    fun dataClassToTs(
        cls: KClass<*>,
        ttm: TsTypeMapper,
        includeTypedInterface: Boolean
    ): String {
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
            " extends " + superClasses.joinToString(", ") { it.simpleName!! }
        } else ""

        val inheritedPropNames = superClasses.flatMap { it.memberProperties }.map { it.name }.toSet()
        val hasTypeProp = cls.memberProperties.any { it.name == "type" }

        val props = cls.memberProperties
            .filter { it.name !in inheritedPropNames }
            .joinToString("\n") { prop ->
                // HVIS feltet heter "type" og vi har slått på includeTypedInterface,
                // tvinger vi type-definisjonen til å bli klassens navn som en string-literal!
                val tsType = if (prop.name == "type" && includeTypedInterface) {
                    "\"${cls.simpleName}\""
                } else {
                    ttm.kotlinToTsType(prop.returnType.toString(), typeParams)
                }
                "  ${prop.name}: $tsType;"
            }

        return buildString {
            appendLine("export interface ${cls.simpleName}$generic$extendsClause {")

            // Hvis klassen IKKE hadde "type"-felt fra før, men flagget er på, legger vi til lappen her
            if (includeTypedInterface && !hasTypeProp && "type" !in inheritedPropNames) {
                appendLine("  type: \"${cls.simpleName}\";")
            }

            if (props.isNotBlank()) appendLine(props)
            appendLine("}")
        }
    }
}