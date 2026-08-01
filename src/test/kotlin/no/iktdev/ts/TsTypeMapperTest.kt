package no.iktdev.ts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


class TsTypeMapperTest {

    @Test
    fun `should map primitives correctly`() {
        assertEquals("string", TsTypeMapper.kotlinToTsType("kotlin.String"))
        assertEquals("number", TsTypeMapper.kotlinToTsType("kotlin.Int"))
        assertEquals("number", TsTypeMapper.kotlinToTsType("kotlin.Double"))
        assertEquals("boolean", TsTypeMapper.kotlinToTsType("kotlin.Boolean"))
    }

    @Test
    fun `should map java types to string`() {
        assertEquals("string", TsTypeMapper.kotlinToTsType("java.util.UUID"))
        assertEquals("string", TsTypeMapper.kotlinToTsType("java.time.Instant"))
    }

    @Test
    fun `should handle nullability`() {
        assertEquals("string | null", TsTypeMapper.kotlinToTsType("kotlin.String?"))
        assertEquals("number | null", TsTypeMapper.kotlinToTsType("kotlin.Int?"))
    }

    @Test
    fun `should map List and Set to arrays`() {
        assertEquals("string[]", TsTypeMapper.kotlinToTsType("kotlin.collections.List<kotlin.String>"))
        assertEquals("number[]", TsTypeMapper.kotlinToTsType("kotlin.collections.MutableSet<kotlin.Int>"))
    }

    @Test
    fun `should handle nested Map`() {
        val input = "kotlin.collections.Map<kotlin.String, kotlin.Int>"
        assertEquals("Record<string, number>", TsTypeMapper.kotlinToTsType(input))
    }

    @Test
    fun `should handle generic parameters`() {
        // Antar at "T" er definert i genericParams
        val generics = listOf("T")
        assertEquals("T[]", TsTypeMapper.kotlinToTsType("kotlin.collections.List<T>", generics))
    }

    @Test
    fun `should handle unknown types by taking simple name`() {
        assertEquals("MyCustomClass", TsTypeMapper.kotlinToTsType("no.iktdev.models.MyCustomClass"))
    }
}