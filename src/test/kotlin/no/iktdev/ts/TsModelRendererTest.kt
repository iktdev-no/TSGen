package no.iktdev.ts

import no.iktdev.ts.models.AChild
import no.iktdev.ts.models.Camera
import no.iktdev.ts.models.DeviceStatus
import no.iktdev.ts.models.Fancy
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TsModelRendererTest {

    private val renderer = TsModelRenderer()
    private val ttm = TsTypeMapper()

    @Test
    fun `should maintain exact property names without renaming`() {
        val result = renderer.dataClassToTs(Fancy::class, ttm, true)

        // Sjekk at "isFancy" ikke ble endret til "fancy"
        assertThat(result).contains("isFancy: boolean;")
        // Sjekk at arv er med
        assertThat(result).contains("extends Parent")
        // Sjekk at arvet felt (id) ikke dupliseres hvis du har filtrert det bort
        assertThat(result).doesNotContain("id: string;")
    }

    @Test
    fun `full test - verifiser arv, typer og union`() {
        // 1. Test Interface Arv
        val cameraTs = renderer.dataClassToTs(Camera::class, ttm, true)

        assertThat(cameraTs).contains("interface Camera extends BaseDevice")
        assertThat(cameraTs).contains("resolution: number;")
        assertThat(cameraTs).doesNotContain("id: string;") // Skal være arvet, ikke duplisert

        // 2. Test Enum til Union Type
        val enumTs = renderer.enumToTs(DeviceStatus::class)
        assertThat(enumTs).isEqualTo("export type DeviceStatus = \"ONLINE\" | \"OFFLINE\"\n")
    }

    @Test
    fun `should handle generic classes correctly`() {
        data class Wrapper<T>(val data: T, val message: String?)

        val result = renderer.dataClassToTs(Wrapper::class, ttm, true)

        assertThat(result).contains("export interface Wrapper<T>")
        assertThat(result).contains("data: T;")
        assertThat(result).contains("message: string | null;")
    }

    @Test
    fun `sealed subtype test - verifiser type discriminator`() {
        data class Sensor(val type: String = "TEMP_SENSOR", val value: Double)

        // Bruker dataClassToTs direkte ettersom sealed subtyper nå bruker denne metoden
        val sensorTs = renderer.dataClassToTs(Sensor::class, ttm, includeTypedInterface = true)

        assertThat(sensorTs).contains("type: \"Sensor\";")
        assertThat(sensorTs).contains("value: number;")
    }

    @Test
    fun `should generate string literal union for enums`() {
        val result = renderer.enumToTs(DeviceStatus::class)

        assertThat(result).isEqualTo("export type DeviceStatus = \"ONLINE\" | \"OFFLINE\"\n")
    }

    @Test
    fun `should handle classes with no unique properties gracefully`() {
        open class Base(val id: String)
        data class EmptySub(val type: String = "EmptySub") : Base("123")

        val result = renderer.dataClassToTs(EmptySub::class, ttm, true)

        assertThat(result).contains("export interface EmptySub extends Base")
        assertThat(result).contains("type: \"EmptySub\";")
    }

    @Test
    fun `should map list of complex objects correctly`() {
        data class SubItem(val code: String)
        data class Container(val items: List<SubItem>)

        val result = renderer.dataClassToTs(Container::class, ttm, false)

        assertThat(result).contains("items: SubItem[];")
    }

    @Test
    fun `should correctly inherit properties from parent data class`() {
        open class Parent(val id: String, val createdAt: String)
        data class Child(val name: String, val age: Int) : Parent("default-id", "now")

        val result = renderer.dataClassToTs(Child::class, ttm, false)

        assertThat(result).contains("extends Parent")
        assertThat(result).contains("name: string;")
        assertThat(result).contains("age: number;")
        // Felt fra Parent skal være filtrert bort fra Child-interfacet
        assertThat(result).doesNotContain("id: string;")
        assertThat(result).doesNotContain("createdAt: string;")
    }

    @Test
    fun `sealed subtype test - no type when off`() {
        data class Sensor(val type: String = "TEMP_SENSOR", val value: Double)

        // Tester med false for å verifisere at type-lappen utelates
        val sensorTs = renderer.dataClassToTs(Sensor::class, ttm, includeTypedInterface = false)

        assertThat(sensorTs).doesNotContain("type: \"Sensor\";")
        assertThat(sensorTs).contains("value: number;")
    }

    @Test
    fun `should correctly extend abstract class and avoid duplicated properties`() {
        // 1. Generer TypeScript fra dummy-klassen
        val result = renderer.dataClassToTs(AChild::class, ttm, true)

        // 2. Verifiser at den utvider den abstrakte klassen
        assertThat(result).contains("extends AParent")

        // 3. Verifiser at felt som finnes i AParent IKKE er duplisert i AChild
        assertThat(result).doesNotContain("macAdress: string;")
        assertThat(result).doesNotContain("interfaceName: string;")

        // 4. Verifiser at unike felt for sub-klassen er med
        assertThat(result).contains("caps: string[];")

        // 5. Sjekk at klassenavnet er riktig
        assertThat(result).contains("export interface AChild")
    }
}