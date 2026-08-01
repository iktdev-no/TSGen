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

    @Test
    fun `should maintain exact property names without renaming`() {
        val result = renderer.dataClassToTs(Fancy::class)

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
        val cameraTs = renderer.dataClassToTs(Camera::class)

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

        val result = renderer.dataClassToTs(Wrapper::class)

        assertThat(result).contains("export interface Wrapper<T>")
        assertThat(result).contains("data: T;")
        assertThat(result).contains("message: string | null;")
    }

    class TsModelRendererTest {
        private val renderer = TsModelRenderer()

        sealed class ASealedDevice
        data class Sensor(val value: Double) : ASealedDevice()
        data class Actuator(val active: Boolean) : ASealedDevice()

        @Test
        fun `sealed union and subtypes - AS_INTERFACE_WITH_TYPE`() {
            // 1. Verifiser at hovedklassen blir et interface + union-type
            val unionTs = renderer.sealedUnionToTs(ASealedDevice::class, SealedStrategy.AS_INTERFACE_WITH_TYPE)
            assertThat(unionTs).contains("export interface ASealedDevice")
            assertThat(unionTs).contains("export type ASealedDeviceType = Actuator | Sensor")

            // 2. Verifiser at subtypen får type-discriminator og arver baseklassen
            val sensorTs = renderer.sealedSubtypeToTs(Sensor::class, SealedStrategy.AS_INTERFACE_WITH_TYPE, "ASealedDevice")
            assertThat(sensorTs).contains("export interface Sensor extends ASealedDevice")
            assertThat(sensorTs).contains("type: \"Sensor\";")
            assertThat(sensorTs).contains("value: number;")
        }

        @Test
        fun `sealed union and subtypes - ONLY_TYPED`() {
            // 1. Verifiser at ONLY_TYPED kun lager en ren union-type for hovedklassen
            val unionTs = renderer.sealedUnionToTs(ASealedDevice::class, SealedStrategy.ONLY_TYPED)
            assertThat(unionTs).isEqualTo("export type ASealedDevice = Actuator | Sensor\n")

            // 2. Verifiser at subtypen IKKE får type-discriminator eller extends-klausul når baseName er null
            val sensorTs = renderer.sealedSubtypeToTs(Sensor::class, SealedStrategy.ONLY_TYPED, null)
            assertThat(sensorTs).doesNotContain("type:")
            assertThat(sensorTs).doesNotContain("extends")
            assertThat(sensorTs).contains("export interface Sensor {")
            assertThat(sensorTs).contains("value: number;")
        }

        @Test
        fun `sealed union and subtypes - AS_INTERFACE`() {
            // 1. Verifiser at AS_INTERFACE lager base-interface og union-type
            val unionTs = renderer.sealedUnionToTs(ASealedDevice::class, SealedStrategy.AS_INTERFACE)
            assertThat(unionTs).contains("export interface ASealedDevice")
            assertThat(unionTs).contains("export type ASealedDeviceType = Actuator | Sensor")

            // 2. Verifiser at subtypen arver baseklassen, men IKKE har automatisk type-discriminator
            val sensorTs = renderer.sealedSubtypeToTs(Sensor::class, SealedStrategy.AS_INTERFACE, "ASealedDevice")
            assertThat(sensorTs).contains("export interface Sensor extends ASealedDevice")
            assertThat(sensorTs).doesNotContain("type:")
            assertThat(sensorTs).contains("value: number;")
        }
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

        val result = renderer.dataClassToTs(EmptySub::class)

        assertThat(result).contains("export interface EmptySub extends Base")
        assertThat(result).contains("type: \"EmptySub\";")
    }

    @Test
    fun `should map list of complex objects correctly`() {
        data class SubItem(val code: String)
        data class Container(val items: List<SubItem>)

        val result = renderer.dataClassToTs(Container::class)

        assertThat(result).contains("items: SubItem[];")
    }

    @Test
    fun `should correctly inherit properties from parent data class`() {
        open class Parent(val id: String, val createdAt: String)
        data class Child(val name: String, val age: Int) : Parent("default-id", "now")

        val result = renderer.dataClassToTs(Child::class)

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
        val sensorTs = renderer.dataClassToTs(Sensor::class)

        assertThat(sensorTs).doesNotContain("type: \"Sensor\";")
        assertThat(sensorTs).contains("value: number;")
    }

    @Test
    fun `should correctly extend abstract class and avoid duplicated properties`() {
        // 1. Generer TypeScript fra dummy-klassen
        val result = renderer.dataClassToTs(AChild::class)

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