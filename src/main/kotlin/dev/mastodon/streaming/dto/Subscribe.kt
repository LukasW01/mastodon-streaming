package dev.mastodon.streaming.dto

import jakarta.json.bind.annotation.JsonbCreator

data class Subscribe @JsonbCreator constructor(
    val type: String,
    val stream: String,
    val list: String?
)
