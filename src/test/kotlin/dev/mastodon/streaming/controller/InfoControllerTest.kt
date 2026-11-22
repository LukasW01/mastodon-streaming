package dev.mastodon.streaming.controller

import io.quarkus.test.junit.QuarkusTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@QuarkusTest
class InfoControllerTest {
    val infoController = InfoController()

    @Test
    fun `test health check`() {
        val response = infoController.health()
        assertEquals("OK", response)
    }
}
