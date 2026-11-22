package dev.mastodon.streaming.dto

import com.fasterxml.jackson.databind.ObjectMapper

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FilteredStatusTest {
    private val objectMapper = ObjectMapper().findAndRegisterModules()

    @Test
    fun `the status is serialized with its filter results`() {
        val status = FilteredStatus(
            objectMapper.readTree("""{"id": "1", "content": "<p>Kotlin</p>", "account": {"id": "2"}}"""),
            listOf(FilterResult(FilterResult.Filter("7", "Languages", listOf("home"), null, "warn"), listOf("Kotlin"))),
        )

        assertEquals(
            objectMapper.readTree(
                """
                {
                  "id": "1",
                  "content": "<p>Kotlin</p>",
                  "account": {"id": "2"},
                  "filtered": [{
                    "filter": {"id": "7", "title": "Languages", "context": ["home"], "expires_at": null, "filter_action": "warn"},
                    "keyword_matches": ["Kotlin"],
                    "status_matches": null
                  }]
                }
                """
            ),
            objectMapper.readTree(objectMapper.writeValueAsString(status)),
        )
    }

    @Test
    fun `unknown filter actions are left out`() {
        val filter = FilterResult.Filter("7", "Languages", listOf("home"), null, null)

        assertEquals(
            objectMapper.readTree("""{"id": "7", "title": "Languages", "context": ["home"], "expires_at": null}"""),
            objectMapper.valueToTree(filter),
        )
    }
}
