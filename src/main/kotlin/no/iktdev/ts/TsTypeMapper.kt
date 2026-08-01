package no.iktdev.ts

object TsTypeMapper {
    fun kotlinToTsType(kotlinType: String, genericParams: List<String> = emptyList()): String {
        val isNullable = kotlinType.endsWith("?")
        // Fjerner eventuelle backticks som Kotlin legger rundt lokale/test-klasser, og trimmet
        val clean = kotlinType.removeSuffix("?").replace("`", "").trim()

        // Sjekk om typen er en generisk parameter (f.eks. "T")
        if (genericParams.contains(clean)) {
            return if (isNullable) "$clean | null" else clean
        }

        // UUID → string
        if (clean == "java.util.UUID") {
            return if (isNullable) "string | null" else "string"
        }

        // Instant → string
        if (clean == "java.time.Instant") {
            return if (isNullable) "string | null" else "string"
        }

        // Duration → string
        if (clean in listOf("kotlin.time.Duration", "java.time.Duration")) {
            return if (isNullable) "string | null" else "string"
        }

        // Map<K, V> → Record<K, V>
        if (clean.startsWith("kotlin.collections.Map") ||
            clean.startsWith("kotlin.collections.MutableMap")) {

            val inner = clean.substringAfter("<").substringBeforeLast(">")
            val (key, value) = inner.split(",").map { it.trim() }

            val tsKey = kotlinToTsType(key, genericParams)
            val tsValue = kotlinToTsType(value, genericParams)

            val base = "Record<$tsKey, $tsValue>"
            return if (isNullable) "$base | null" else base
        }

        // Set<T> → T[]
        if (clean.startsWith("kotlin.collections.Set") ||
            clean.startsWith("kotlin.collections.MutableSet")) {

            val inner = clean.substringAfter("<").substringBeforeLast(">")

            if (genericParams.contains(inner)) {
                val base = "${inner}[]"
                return if (isNullable) "$base | null" else base
            }

            val tsInner = kotlinToTsType(inner, genericParams)
            val base = "$tsInner[]"
            return if (isNullable) "$base | null" else base
        }

        // List<T> → T[]
        if (clean.startsWith("kotlin.collections.List") ||
            clean.startsWith("kotlin.collections.MutableList")) {

            val inner = clean.substringAfter("<").substringBeforeLast(">")

            if (genericParams.contains(inner)) {
                val base = "${inner}[]"
                return if (isNullable) "$base | null" else base
            }

            val tsInner = kotlinToTsType(inner, genericParams)
            val base = "$tsInner[]"
            return if (isNullable) "$base | null" else base
        }

        // Primitive mappings / Klasser
        val base = when (clean) {
            "kotlin.String" -> "string"
            "kotlin.Int",
            "kotlin.Long",
            "kotlin.Float",
            "kotlin.Double" -> "number"
            "kotlin.Boolean" -> "boolean"
            "kotlin.Any" -> "any"
            else -> {
                // Stripper vekk alt av pakkenavn og eventuelle lokale test-referanser med $
                clean.substringAfterLast(".").substringAfterLast("$")
            }
        }

        return if (isNullable) "$base | null" else base
    }
}