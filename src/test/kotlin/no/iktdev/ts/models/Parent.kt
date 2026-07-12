package no.iktdev.ts.models

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

// Dummy-klasser for testing
interface Parent {
    val id: String
}
data class Fancy(val isFancy: Boolean) : Parent {
    override val id: String = "123"
}

interface BaseDevice {
    val id: String
}

data class Camera(override val id: String, val resolution: Int) : BaseDevice

enum class DeviceStatus { ONLINE, OFFLINE }