package dev.mastodon.streaming.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.quarkus.runtime.annotations.RegisterForReflection
import jakarta.json.bind.annotation.JsonbCreator

/** Over HTTP only the `error` is sent, websocket clients also receive the `status`. */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponse @JsonbCreator constructor(
    val error: String,
    val status: Int? = null,
)

data class InfoResponse @JsonbCreator constructor(
    val text: String,
    val status: Int,
)
