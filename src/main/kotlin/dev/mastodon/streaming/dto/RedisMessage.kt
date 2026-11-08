package dev.mastodon.streaming.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import io.quarkus.runtime.annotations.RegisterForReflection

/**
 * An object with an `event` and `payload` property. Some events also include a
 * queued_at value, but this is being removed shortly.
 */
@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
data class RedisMessage(
    @JsonProperty("event") val event: String? = null,
    @JsonProperty("payload") val payload: JsonNode? = null,
    @JsonProperty("queued_at") val queuedAt: Long? = null,
)
