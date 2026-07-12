package no.iktdev.ts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


class TsTypeMapperTest {

    private val mapper = TsTypeMapper()

    @Test
    fun `should map primitives correctly`() {
        assertEquals("string", mapper.kotlinToTsType("kotlin.String"))
        assertEquals("number", mapper.kotlinToTsType("kotlin.Int"))
        assertEquals("number", mapper.kotlinToTsType("kotlin.Double"))
        assertEquals("boolean", mapper.kotlinToTsType("kotlin.Boolean"))
    }

    @Test
    fun `should map java types to string`() {
        assertEquals("string", mapper.kotlinToTsType("java.util.UUID"))
        assertEquals("string", mapper.kotlinToTsType("java.time.Instant"))
    }

    @Test
    fun `should handle nullability`() {
        assertEquals("string | null", mapper.kotlinToTsType("kotlin.String?"))
        assertEquals("number | null", mapper.kotlinToTsType("kotlin.Int?"))
    }

    @Test
    fun `should map List and Set to arrays`() {
        assertEquals("string[]", mapper.kotlinToTsType("kotlin.collections.List<kotlin.String>"))
        assertEquals("number[]", mapper.kotlinToTsType("kotlin.collections.MutableSet<kotlin.Int>"))
    }

    @Test
    fun `should handle nested Map`() {
        val input = "kotlin.collections.Map<kotlin.String, kotlin.Int>"
        assertEquals("Record<string, number>", mapper.kotlinToTsType(input))
    }

    @Test
    fun `should handle generic parameters`() {
        // Antar at "T" er definert i genericParams
        val generics = listOf("T")
        assertEquals("T[]", mapper.kotlinToTsType("kotlin.collections.List<T>", generics))
    }

    @Test
    fun `should handle unknown types by taking simple name`() {
        assertEquals("MyCustomClass", mapper.kotlinToTsType("no.iktdev.models.MyCustomClass"))
    }
}