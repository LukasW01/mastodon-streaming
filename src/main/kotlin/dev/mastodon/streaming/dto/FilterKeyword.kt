package dev.mastodon.streaming.dto

import io.quarkus.runtime.annotations.RegisterForReflection
import jakarta.json.bind.annotation.JsonbCreator
import java.time.LocalDateTime

/** The database returns one row per keyword, [CachedFilter.fromKeywords] groups them by filter. */
@RegisterForReflection
data class FilterKeyword @JsonbCreator constructor(
    val id: Long,
    val phrase: String,
    val context: List<String>,
    val expiresAt: LocalDateTime?,
    val filterAction: Int,
    val keyword: String,
    val wholeWord: Boolean
)
