package dev.mastodon.streaming.dto

import java.time.LocalDateTime

data class CachedFilter(
    val id: Long,
    val title: String,
    val context: List<String>,
    val expiresAt: LocalDateTime?,
    val filterAction: String,
    val regex: Regex,
)
