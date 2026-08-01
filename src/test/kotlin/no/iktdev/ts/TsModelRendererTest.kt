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
    fun `sealed subtype test - verifiser type discriminator`() {
        data class Sensor(val type: String = "TEMP_SENSOR", val value: Double)

        // Send med true for å inkludere type-diskriminatoren i testen
        val sensorTs = renderer.sealedSubtypeToTs(Sensor::class, ttm, includeTypeDiscriminator = true)

        assertThat(sensorTs).contains("type: \"Sensor\";")
        assertThat(sensorTs).contains("value: number;")
    }

    @Test
    fun `sealed subtype test - no type when off`() {
        data class Sensor(val type: String = "TEMP_SENSOR", val value: Double)

        // Send med true for å inkludere type-diskriminatoren i testen
        val sensorTs = renderer.sealedSubtypeToTs(Sensor::class, ttm, includeTypeDiscriminator = false)

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
        // Dette bekrefter at filtreringen mot superclass fungerer
        assertThat(result).doesNotContain("macAdress: string;")
        assertThat(result).doesNotContain("interfaceName: string;")

        // 4. Verifiser at unike felt for sub-klassen er med
        // (Merk: ttm mapper List til string[] i din originale kode)
        assertThat(result).contains("caps: string[];")

        // 5. Sjekk at klassenavnet er riktig
        assertThat(result).contains("export interface AChild")
    }
}