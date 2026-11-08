package dev.mastodon.streaming.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.quarkus.runtime.annotations.RegisterForReflection

data class StreamParams(
    val tag: String? = null,
    val list: String? = null,
)

/**
 * The message websocket clients send to subscribe to or unsubscribe from a
 * stream. Clients send `stream` either as a string or as an array.
 */
@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
data class Subscribe(
    val type: String? = null,
    @field:JsonFormat(with = [JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY])
    val stream: List<String>? = null,
    val tag: String? = null,
    val list: String? = null,
)
