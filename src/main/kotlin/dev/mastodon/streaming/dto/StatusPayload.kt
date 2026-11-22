package dev.mastodon.streaming.dto

import dev.mastodon.streaming.util.primitives.RegexPattern

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import io.quarkus.runtime.annotations.RegisterForReflection
import org.jsoup.Jsoup

/**
 * The payload of `update` and `status.update` events, which is a Status entity.
 * Only the properties needed for filtering are read, the payload transmitted to
 * the client stays the one received from Redis.
 */
@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
data class StatusPayload(
    val id: String? = null,
    val language: String? = null,
    val account: Account? = null,
    val mentions: List<Mention>? = null,
    val poll: Poll? = null,
    @JsonProperty("media_attachments") val mediaAttachments: List<MediaAttachment>? = null,
    @JsonProperty("spoiler_text") val spoilerText: String? = null,
    val content: String? = null,
    val filtered: JsonNode? = null,
) {
    fun isLocal(): Boolean = account?.username == account?.acct

    fun authorId(): Long = account?.id ?: 0

    fun accountDomain(): String? = account?.acct?.split('@')?.getOrNull(1)

    fun targetAccountIds(): List<Long> = listOf(authorId()) + mentions.orEmpty().map { it.id }

    // If the payload already contains the `filtered` property, it means that
    // filtering has been applied on the ruby on rails side, as such, we don't
    // need to construct or apply the filters in streaming.
    fun isFiltered(): Boolean = filtered != null

    fun searchableText(): String {
        buildList {
            addAll(listOf(this@StatusPayload.spoilerText.orEmpty(), this@StatusPayload.content.orEmpty()))
            this@StatusPayload.poll?.options.orEmpty().mapTo(this) { it.title.orEmpty() }
            this@StatusPayload.mediaAttachments.orEmpty().mapTo(this) { it.description.orEmpty() }
        }
            .joinToString("\n\n")
            .replace(RegexPattern.BR_TAG.regex, "\n")
            .replace("</p><p>", "\n\n")
            .let { searchableContent ->
                return Jsoup.parseBodyFragment(searchableContent).body().wholeText()
            }
    }

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Account(
        val id: Long = 0,
        val username: String? = null,
        val acct: String? = null,
    )

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Mention(
        val id: Long = 0,
    )

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Poll(
        val options: List<PollOption>? = null,
    )

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class PollOption(
        val title: String? = null,
    )

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class MediaAttachment(
        val description: String? = null,
    )
}
