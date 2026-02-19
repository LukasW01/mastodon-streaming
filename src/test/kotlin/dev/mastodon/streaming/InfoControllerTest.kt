package dev.mastodon.streaming

import dev.mastodon.streaming.controller.InfoController
import dev.mastodon.streaming.dto.InfoResponse
import io.quarkus.test.junit.QuarkusTest
import jakarta.ws.rs.core.Response
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@QuarkusTest
class InfoControllerTest {
    private val infoController = InfoController()

    @Test
    fun testHealth() {
        val response = infoController.health()
        assertEquals(Response.Status.OK.statusCode, response.status)
        assertEquals(InfoResponse("OK", Response.Status.OK.statusCode), response)
    }
}
