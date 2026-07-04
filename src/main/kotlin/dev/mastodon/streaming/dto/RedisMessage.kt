package dev.mastodon.streaming.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode

data class RedisMessage(
    @JsonProperty("event") val event: String,
    @JsonProperty("payload") val payload: JsonNode? = null,
    @JsonProperty("queued_at") val queuedAt: Long? = null,
)
