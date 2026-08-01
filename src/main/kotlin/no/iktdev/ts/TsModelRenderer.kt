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

    // Genererer base-interface (hvis ikke ONLY_TYPED) og union-typen
    fun sealedUnionToTs(
        cls: KClass<*>,
        strategy: SealedStrategy
    ): String {
        val subTypes = cls.sealedSubclasses
            .mapNotNull { it.simpleName }
            .joinToString(" | ")

        if (strategy == SealedStrategy.ONLY_TYPED) {
            return "export type ${cls.simpleName} = $subTypes\n"
        }

        // AS_INTERFACE og AS_INTERFACE_WITH_TYPE lager base-interfacet for sealed-klassen
        val superClasses = getSuperClasses(cls)
        val extendsClause = getExtendsClause(superClasses, null)
        val inheritedPropNames = superClasses.flatMap { it.memberProperties }.map { it.name }.toSet()

        val baseInterface = buildInterfaceString(cls, inheritedPropNames, extendsClause, null)
        val unionType = "export type ${cls.simpleName}Ref = $subTypes\n"

        return listOf(baseInterface, unionType).filter { it.isNotBlank() }.joinToString("\n")
    }

    // Sealed subtype renderer som respekterer om vi skal ha type-felt eller ikke
    fun sealedSubtypeToTs(
        cls: KClass<*>,
        strategy: SealedStrategy,
        extendsBaseName: String?
    ): String {
        // Hvis ONLY_TYPED, skal den ikke arve fra eller filtrere ut felter fra baseklassen
        val superClasses = if (strategy == SealedStrategy.ONLY_TYPED) emptyList() else getSuperClasses(cls)

        // Kun AS_INTERFACE_WITH_TYPE og AS_INTERFACE skal arve fra baseklassen
        val extraExtends = if (strategy != SealedStrategy.ONLY_TYPED) extendsBaseName else null
        val extendsClause = getExtendsClause(superClasses, extraExtends)
        val inheritedPropNames = if (strategy == SealedStrategy.ONLY_TYPED) emptySet() else superClasses.flatMap { it.memberProperties }.map { it.name }.toSet()

        // Type-literalen tvinges KUN på hvis strategien er AS_INTERFACE_WITH_TYPE
        val typeLiteral = if (strategy == SealedStrategy.AS_INTERFACE_WITH_TYPE) {
            getDefaultTypeLiteral(cls) ?: cls.simpleName
        } else {
            null
        }

        return buildInterfaceString(cls, inheritedPropNames, extendsClause, typeLiteral)
    }

    // Ordinær data-klasse / interface renderer
    fun dataClassToTs(cls: KClass<*>): String {
        val superClasses = getSuperClasses(cls)
        val extendsClause = getExtendsClause(superClasses, null)
        val inheritedPropNames = superClasses.flatMap { it.memberProperties }.map { it.name }.toSet()

        return buildInterfaceString(cls, inheritedPropNames, extendsClause, null)
    }

    // --- Felles hjelpemetoder ---

    private fun getSuperClasses(cls: KClass<*>): List<KClass<*>> {
        val superClasses = mutableListOf<KClass<*>>()
        val superClass = cls.java.superclass?.kotlin
        if (superClass != null && superClass != Any::class) {
            superClasses.add(superClass)
        }

        val superInterfaces = cls.java.interfaces
            .map { it.kotlin }
            .filter { it.simpleName != "Any" && it.simpleName != "Serializable" }

        superClasses.addAll(superInterfaces)
        return superClasses
    }

    private fun getExtendsClause(superClasses: List<KClass<*>>, extraExtends: String?): String {
        val names = superClasses.map { it.simpleName!! }.toMutableList()
        if (extraExtends != null && extraExtends !in names) {
            names.add(extraExtends)
        }
        return if (names.isNotEmpty()) {
            " extends ${names.distinct().joinToString(", ")}"
        } else ""
    }

    private fun buildInterfaceString(
        cls: KClass<*>,
        inheritedPropNames: Set<String>,
        extendsClause: String?,
        forcedTypeLiteral: String?
    ): String {
        val typeParams = cls.typeParameters.map { it.name }
        val generic = if (typeParams.isNotEmpty()) "<" + typeParams.joinToString(", ") + ">" else ""
        val defaultTypeLiteral = getDefaultTypeLiteral(cls)

        val props = cls.memberProperties
            .filter { it.name !in inheritedPropNames }
            .filter { forcedTypeLiteral == null || it.name != "type" }
            .joinToString("\n") { prop ->
                val tsType = if (prop.name == "type" && defaultTypeLiteral != null) {
                    "\"$defaultTypeLiteral\""
                } else {
                    TsTypeMapper.kotlinToTsType(prop.returnType.toString(), typeParams)
                }
                "  ${prop.name}: $tsType;"
            }

        val propsBlock = if (props.isBlank()) "" else "\n$props"

        return buildString {
            appendLine("export interface ${cls.simpleName}$generic${extendsClause ?: ""} {")
            if (forcedTypeLiteral != null) {
                appendLine("  type: \"$forcedTypeLiteral\";$propsBlock")
            } else if (props.isNotBlank()) {
                appendLine(props)
            }
            appendLine("}")
        }
    }

    private fun getDefaultTypeLiteral(cls: KClass<*>): String? {
        val ctor = cls.primaryConstructor ?: return null
        ctor.parameters.find { it.name == "type" } ?: return null

        return try {
            val instance = ctor.callBy(emptyMap())
            val prop = cls.memberProperties.find { it.name == "type" }
            prop?.getter?.call(instance)?.toString()
        } catch (_: Throwable) {
            null
        }
    }
}