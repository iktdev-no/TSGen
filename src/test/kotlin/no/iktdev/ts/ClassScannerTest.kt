package no.iktdev.ts

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ClassScannerTest {

    @Test
    fun `should scan package and find this class`() {
        val scanner = ClassScanner()
        val classes = scanner.scan("no.iktdev.ts", this.javaClass.classLoader)

        // Sjekk at vi fant minst oss selv
        assertThat(classes).anyMatch { it.simpleName == "ClassScanner" }
    }
}