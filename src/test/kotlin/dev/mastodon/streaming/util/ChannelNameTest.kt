package dev.mastodon.streaming.util

import dev.mastodon.streaming.dto.ChannelResolution
import dev.mastodon.streaming.dto.StreamParams
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource

class ChannelNameTest {
    @ParameterizedTest
    @EnumSource(ChannelName::class)
    fun fromValueResolvesEveryChannelByItsValue(channel: ChannelName) {
        assertEquals(channel, ChannelName.fromValue(channel.value))
    }

    @Test
    fun `fromValue returns null for unknown or missing values`() {
        assertNull(ChannelName.fromValue("public:everything"))
        assertNull(ChannelName.fromValue(""))
        assertNull(ChannelName.fromValue(null))
    }

    @ParameterizedTest
    @CsvSource(
        "user, false, USER",
        "user, true, USER",
        "user/notification, false, USER_NOTIFICATION",
        "public, false, PUBLIC",
        "public, true, PUBLIC_MEDIA",
        "public/local, false, PUBLIC_LOCAL",
        "public/local, true, PUBLIC_LOCAL_MEDIA",
        "public/remote, false, PUBLIC_REMOTE",
        "public/remote, true, PUBLIC_REMOTE_MEDIA",
        "hashtag, false, HASHTAG",
        "hashtag/local, false, HASHTAG_LOCAL",
        "direct, false, DIRECT",
        "list, false, LIST",
    )
    fun fromPathMapsRequestPathsToChannels(
        path: String,
        onlyMedia: Boolean,
        expected: ChannelName
    ) {
        assertEquals(expected, ChannelName.fromPath(path, onlyMedia))
    }

    @Test
    fun `fromPath returns null for unknown paths`() {
        assertNull(ChannelName.fromPath("system", false))
        assertNull(ChannelName.fromPath("public/media", false))
        assertNull(ChannelName.fromPath("public:local", false))
    }

    @Test
    fun `list and hashtag stream names include their parameter`() {
        assertEquals(listOf("list", "5"), ChannelName.LIST.streamName(StreamParams(list = "5")))
        assertEquals(listOf("hashtag", "Kotlin"), ChannelName.HASHTAG.streamName(StreamParams(tag = "Kotlin")))
        assertEquals(listOf("hashtag:local", "Kotlin"), ChannelName.HASHTAG_LOCAL.streamName(StreamParams(tag = "Kotlin")))
    }

    @Test
    fun `stream names without parameter only contain the channel`() {
        assertEquals(listOf("list"), ChannelName.LIST.streamName(StreamParams()))
        assertEquals(listOf("hashtag"), ChannelName.HASHTAG.streamName(StreamParams(tag = "")))
        assertEquals(listOf("public"), ChannelName.PUBLIC.streamName(StreamParams(tag = "Kotlin", list = "5")))
    }

    @Test
    fun `channel resolutions use the stream name of their channel`() {
        val resolution = ChannelResolution(ChannelName.HASHTAG, StreamParams(tag = "Kotlin"), listOf("timeline:hashtag:kotlin"), needsFiltering = true)

        assertEquals(listOf("hashtag", "Kotlin"), resolution.streamName)
    }
}
