package dev.mastodon.streaming.dto

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.databind.JsonNode
import io.quarkus.runtime.annotations.RegisterForReflection

/**
 * The payload + the FilterResults as the `filtered` property. To reach this
 * code, the `event` must have been either `update` or `status.update`,
 * meaning the `payload` is a Status entity, which has a `filtered` property:
 *
 * filtered: https://docs.joinmastodon.org/entities/Status/#filtered
 *
 * The status is only read, as the same message is shared by all listeners.
 */
@RegisterForReflection
class FilteredStatus(
    private val status: JsonNode,
    val filtered: List<FilterResult>,
) {
    @JsonAnyGetter
    fun properties(): Map<String, JsonNode> = status.properties().associate { it.toPair() }
}
