package dev.mastodon.streaming.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.quarkus.runtime.annotations.RegisterForReflection

/**
 * status_matches is always null as we only are only applying the keyword-based
 * custom filters, not the status-based custom filters.
 * https://docs.joinmastodon.org/entities/FilterResult/
 */
@RegisterForReflection
data class FilterResult(
    val filter: Filter,
    @get:JsonProperty("keyword_matches") val keywordMatches: List<String>,
    @get:JsonProperty("status_matches") val statusMatches: List<String>? = null,
) {
    @RegisterForReflection
    data class Filter(
        val id: String,
        val title: String,
        val context: List<String>,
        @get:JsonProperty("expires_at") val expiresAt: String?,
        @get:JsonProperty("filter_action") @get:JsonInclude(JsonInclude.Include.NON_NULL) val filterAction: String?,
    )
}
