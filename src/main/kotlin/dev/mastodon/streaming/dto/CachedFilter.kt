package dev.mastodon.streaming.dto

import dev.mastodon.streaming.util.Filter
import dev.mastodon.streaming.util.primitives.RegexPattern
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** A custom filter of an account with all of its keywords compiled into a single regular expression. */
data class CachedFilter(
    val id: Long,
    val title: String,
    val context: List<String>,
    val expiresAt: LocalDateTime?,
    val filterAction: String?,
    val regex: Regex,
) {

    fun isExpired(now: LocalDateTime): Boolean = expiresAt != null && expiresAt < now

    fun match(text: String): FilterResult? = regex.find(text)?.let { keywordMatch ->
        FilterResult(
            filter = FilterResult.Filter(id.toString(), title, context, expiresAt?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")), filterAction),
            keywordMatches = listOf(keywordMatch.value),
        )
    }

    companion object {
        /**
         * Construct the regular expressions for the custom filters: This needs
         * to be done after grouping as the database returns one row per keyword,
         * so we need all the keywords before constructing the regular expression
         */
        fun fromKeywords(rows: List<FilterKeyword>): Map<Long, CachedFilter> = rows.groupBy { it.id }.mapValues { (id, keywords) ->
            CachedFilter(
                id = id,
                title = keywords.first().phrase,
                context = keywords.first().context,
                expiresAt = keywords.first().expiresAt,
                filterAction = Filter.TYPE.getOrNull(keywords.first().filterAction),
                regex = Regex(keywords.joinToString("|", transform = ::keywordExpression), RegexOption.IGNORE_CASE),
            )
        }

        private fun keywordExpression(row: FilterKeyword): String {
            val expr = row.keyword.replace(RegexPattern.SPECIAL_CHARS.regex) { """\${it.value}""" }

            if (!row.wholeWord) return expr

            val prefix = if (RegexPattern.STARTS_WITH_WORD_CHAR.regex.containsMatchIn(expr)) """\b""" else ""
            val suffix = if (RegexPattern.ENDS_WITH_WORD_CHAR.regex.containsMatchIn(expr)) """\b""" else ""

            return "$prefix$expr$suffix"
        }
    }
}
