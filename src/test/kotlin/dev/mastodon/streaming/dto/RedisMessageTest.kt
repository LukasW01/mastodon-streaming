package dev.mastodon.streaming.dto

import com.fasterxml.jackson.databind.ObjectMapper

import com.fasterxml.jackson.databind.node.TextNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RedisMessageTest {
    private val objectMapper = ObjectMapper().findAndRegisterModules()

    @Test
    fun `object payloads are encoded as json`() {
        assertEquals("""{"id":"1","content":"Hello"}""", RedisMessage("update", objectMapper.readTree("""{"id": "1", "content": "Hello"}""")).encodedPayload)
    }

    @Test
    fun `string payloads are transmitted as they are`() {
        assertEquals("109", RedisMessage("delete", TextNode("109")).encodedPayload)
    }

    @Test
    fun `missing payloads are encoded as null`() {
        assertEquals("null", RedisMessage("delete").encodedPayload)
    }

    @Test
    fun `messages are deserialized ignoring unknown properties`() {
        val message = objectMapper.readValue("""{"event": "delete", "payload": "109", "queued_at": 1700000000000, "extra": true}""", RedisMessage::class.java)

        assertEquals(RedisMessage("delete", TextNode("109"), 1_700_000_000_000), message)
    }
}
