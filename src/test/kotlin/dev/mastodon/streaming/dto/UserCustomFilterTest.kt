package dev.mastodon.streaming.dto

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

import java.time.LocalDateTime

class UserCustomFilterTest {
    private val keyword = FilterKeyword(
        id = 1,
        phrase = "Languages",
        context = listOf("home", "public"),
        expiresAt = null,
        filterAction = 0,
        keyword = "kotlin",
        wholeWord = false,
    )

    private fun filterOf(vararg keywords: FilterKeyword): UserCustomFilter = UserCustomFilter.fromKeywords(keywords.toList()).values.single()

    @Test
    fun `keywords are grouped by their filter`() {
        val filters = UserCustomFilter.fromKeywords(listOf(
            keyword,
            keyword.copy(keyword = "java"),
            keyword.copy(id = 2, phrase = "Spoilers", filterAction = 1, keyword = "finale"),
        ))

        assertEquals(setOf(1L, 2L), filters.keys)
        assertEquals("Languages", filters.getValue(1).title)
        assertEquals(listOf("home", "public"), filters.getValue(1).context)
        assertEquals("warn", filters.getValue(1).filterAction)
        assertEquals("hide", filters.getValue(2).filterAction)
        assertNotNull(filters.getValue(1).match("I like Java"))
        assertNull(filters.getValue(2).match("I like Java"))
    }

    @Test
    fun `unknown filter actions are left out`() {
        assertNull(filterOf(keyword.copy(filterAction = 5)).filterAction)
    }

    @Test
    fun `keywords match case insensitively`() {
        assertEquals(listOf("KOTLIN"), filterOf(keyword).match("I LOVE KOTLIN")?.keywordMatches)
    }

    @Test
    fun `special characters in keywords are matched literally`() {
        val filter = filterOf(keyword.copy(keyword = "c++"), keyword.copy(keyword = "a.b"))

        assertEquals(listOf("C++"), filter.match("I write C++")?.keywordMatches)
        assertNull(filter.match("axb"))
    }

    @Test
    fun `whole word keywords only match whole words`() {
        val filter = filterOf(keyword.copy(keyword = "cat", wholeWord = true))

        assertNotNull(filter.match("my cat sleeps"))
        assertNull(filter.match("concatenate"))
    }

    @Test
    fun `other keywords also match within words`() {
        assertNotNull(filterOf(keyword.copy(keyword = "cat")).match("concatenate"))
    }

    @Test
    fun `whole word boundaries are only added next to word characters`() {
        assertNotNull(filterOf(keyword.copy(keyword = "#tag", wholeWord = true)).match("see #tag here"))
        assertNotNull(filterOf(keyword.copy(keyword = "c++", wholeWord = true)).match("c++ rocks"))
    }

    @Test
    fun `match describes the filter that matched`() {
        val result = filterOf(keyword.copy(expiresAt = LocalDateTime.parse("2030-01-02T03:04:05"))).match("Kotlin is fun")

        assertEquals(
            FilterResult(
                filter = FilterResult.Filter("1", "Languages", listOf("home", "public"), "2030-01-02T03:04:05.000Z", "warn"),
                keywordMatches = listOf("Kotlin"),
            ),
            result,
        )
    }

    @Test
    fun `match returns null when no keyword matches`() {
        assertNull(filterOf(keyword).match("Nothing to see here"))
    }

    @Test
    fun `filters expire after their expiry date`() {
        val now = LocalDateTime.parse("2026-01-01T12:00")

        assertFalse(filterOf(keyword).isExpired(now))
        assertFalse(filterOf(keyword.copy(expiresAt = now.plusMinutes(1))).isExpired(now))
        assertTrue(filterOf(keyword.copy(expiresAt = now.minusMinutes(1))).isExpired(now))
    }
}
