package dev.mastodon.streaming.dto

import jakarta.json.bind.annotation.JsonbCreator

data class AccessToken @JsonbCreator constructor(
    val tokenId: Long,
    val resourceOwnerId: Long,
    val accountId: Long,
    val chosenLanguages: List<String>? = null,
    val scopes: String,
    val permissions: Int,
)
