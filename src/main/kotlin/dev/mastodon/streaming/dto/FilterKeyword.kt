package dev.mastodon.streaming.dto

import jakarta.json.bind.annotation.JsonbCreator
import java.time.LocalDateTime

data class FilterKeyword @JsonbCreator constructor(
    val id: Long,
    val phrase: String,
    val context: List<String>,
    val expiresAt: LocalDateTime,
    val filterAction: Int,
    val keyword: String,
    val wholeWord: Boolean
)
