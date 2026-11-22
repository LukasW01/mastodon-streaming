package dev.mastodon.streaming.util.mapper

import dev.mastodon.streaming.dto.ErrorResponse
import jakarta.ws.rs.core.MediaType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WebApplicationMapperExceptionTest {
    @Test
    fun `the response carries the status and the error as json`() {
        val err = WebApplicationMapperException("Missing access token", 401)

        assertEquals("Missing access token", err.message)
        assertEquals(401, err.status)
        assertEquals(401, err.response.status)
        assertEquals(MediaType.APPLICATION_JSON_TYPE, err.response.mediaType)
        assertEquals(ErrorResponse("Missing access token"), err.response.entity)
    }

    @Test
    fun `mapper exceptions expose their message and status`() {
        assertEquals(ErrorResponse("Unknown stream type", 400), WebApplicationMapperException("Unknown stream type", 400).toErrorResponse())
    }

    @Test
    fun `other exceptions are hidden behind an internal server error`() {
        assertEquals(ErrorResponse("Internal Server Error", 500), IllegalStateException("database password is wrong").toErrorResponse())
    }
}
