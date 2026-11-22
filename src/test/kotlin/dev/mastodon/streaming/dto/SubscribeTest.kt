package dev.mastodon.streaming.dto

import dev.mastodon.streaming.jsonMapper

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SubscribeTest {
    private val objectMapper = jsonMapper()

    @Test
    fun `stream can be sent as a string`() {
        assertEquals(
            Subscribe(type = "subscribe", stream = listOf("public")),
            objectMapper.readValue("""{"type": "subscribe", "stream": "public"}""", Subscribe::class.java),
        )
    }

    @Test
    fun `stream can be sent as an array`() {
        assertEquals(
            Subscribe(type = "subscribe", stream = listOf("hashtag", "kotlin"), tag = "kotlin"),
            objectMapper.readValue("""{"type": "subscribe", "stream": ["hashtag", "kotlin"], "tag": "kotlin"}""", Subscribe::class.java),
        )
    }

    @Test
    fun `unknown properties are ignored`() {
        assertEquals(
            Subscribe(type = "unsubscribe", stream = listOf("list"), list = "5"),
            objectMapper.readValue("""{"type": "unsubscribe", "stream": "list", "list": "5", "unknown": 1}""", Subscribe::class.java),
        )
    }
}
