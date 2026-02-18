package dev.mastodon.streaming.dto

import jakarta.json.bind.annotation.JsonbCreator

data class ErrorResponse @JsonbCreator constructor(
    val text: String,
    val status: Int,
)

data class InfoResponse @JsonbCreator constructor(
    val text: String,
    val status: Int,
)
