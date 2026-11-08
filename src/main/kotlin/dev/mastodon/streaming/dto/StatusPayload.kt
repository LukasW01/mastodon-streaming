package dev.mastodon.streaming.dto

import dev.mastodon.streaming.util.primitives.RegexPattern

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.jsoup.Jsoup

/** The payload of `update` and `status.update` events, which is a Status entity. */
class StatusPayload(private val json: JsonNode) {
    val id: String = json.path("id").asText()
    val language: String? = json.path("language").textValue()
    val isLocal: Boolean = json.path("account").path("username").asText() == json.path("account").path("acct").asText()
    val authorId: Long = json.path("account").path("id").asLong()
    val accountDomain: String? = json.path("account")
        .path("acct")
        .asText()
        .split('@')
        .getOrNull(1)
    val targetAccountIds: List<Long> = listOf(authorId) + json.path("mentions").map { it.path("id").asLong() }
    val pollOptions = json.path("poll").path("options").map { it.path("title").asText("") }
    val descriptions = json.path("media_attachments").map { it.path("description").asText("") }
    val searchableContent = (listOf(json.path("spoiler_text").asText(""), json.path("content").asText("")) + pollOptions + descriptions).joinToString("\n\n").replace(
        RegexPattern.BR_TAG.regex, "\n"
    ).replace("</p><p>", "\n\n")

    // If the payload already contains the `filtered` property, it means that
    // filtering has been applied on the ruby on rails side, as such, we don't
    // need to construct or apply the filters in streaming.
    val isFiltered: Boolean = json.has("filtered")

    fun searchableText(): String = Jsoup.parseBodyFragment(searchableContent).body().wholeText()

    /**
     * The payload + the FilterResults as the `filtered` property. To reach this
     * code, the `event` must have been either `update` or `status.update`,
     * meaning the `payload` is a Status entity, which has a `filtered` property:
     *
     * filtered: https://docs.joinmastodon.org/entities/Status/#filtered
     *
     * This is a shallow copy, as the same message is shared by all listeners.
     */
    fun withFilterResults(results: List<FilterResult>, objectMapper: ObjectMapper): JsonNode =
        objectMapper.createObjectNode().apply {
            setAll<ObjectNode>(json as ObjectNode)
            set<ObjectNode>("filtered", objectMapper.valueToTree(results))
        }
}
