package dev.mastodon.streaming.util.primitives

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class StringTest {
    @Test
    fun `foldToAscii replaces accented characters`() {
        assertEquals("Nandu", "Ñandú".foldToAscii())
        assertEquals("Aeiou", "Äéìõů".foldToAscii())
    }

    @Test
    fun `foldToAscii keeps characters without ascii equivalent`() {
        assertEquals("Straße 日本", "Straße 日本".foldToAscii())
    }

    @ParameterizedTest
    @CsvSource(
        "Kotlin, kotlin",
        "#Kotlin, kotlin",
        "Café, cafe",
        "hello-world!, helloworld",
        "snake_case, snake_case",
        "ＫＯＴＬＩＮ, kotlin",
        "日本語, 日本語",
        "l·l, l·l",
    )
    fun normalizeHashtagMatchesMastodon(tag: String, expected: String) {
        assertEquals(expected, tag.normalizeHashtag())
    }
}
