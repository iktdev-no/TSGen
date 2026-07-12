package no.iktdev.ts

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
        val result = renderer.dataClassToTs(Fancy::class, ttm)

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
        val cameraTs = renderer.dataClassToTs(Camera::class, ttm)

        assertThat(cameraTs).contains("interface Camera extends BaseDevice")
        assertThat(cameraTs).contains("resolution: number;")
        assertThat(cameraTs).doesNotContain("id: string;") // Skal være arvet, ikke duplisert

        // 2. Test Enum til Union Type
        val enumTs = renderer.enumToTs(DeviceStatus::class)
        assertThat(enumTs).isEqualTo("export type DeviceStatus = \"ONLINE\" | \"OFFLINE\"\n")
    }

    @Test
    fun `sealed subtype test - verifiser type discriminator`() {
        // La oss si vi har en klasse som bruker sealed-logikken din
        data class Sensor(val type: String = "TEMP_SENSOR", val value: Double)

        val sensorTs = renderer.sealedSubtypeToTs(Sensor::class)

        assertThat(sensorTs).contains("type: \"Sensor\";")
        assertThat(sensorTs).contains("value: number;")
    }
}