package dev.mastodon.streaming.dto

import io.quarkus.runtime.annotations.RegisterForReflection

/** The message websocket clients receive, the payload is an encoded JSON string. */
@RegisterForReflection
data class StreamMessage(
    val stream: List<String>,
    val event: String,
    val payload: String,
)
