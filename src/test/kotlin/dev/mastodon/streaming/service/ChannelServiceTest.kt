package dev.mastodon.streaming.service

import dev.mastodon.streaming.db.list.ListService
import dev.mastodon.streaming.db.setting.SettingService
import dev.mastodon.streaming.dto.AccessToken
import dev.mastodon.streaming.dto.FeedSetting
import dev.mastodon.streaming.dto.StreamParams
import dev.mastodon.streaming.util.ChannelName
import dev.mastodon.streaming.util.Permissions
import dev.mastodon.streaming.util.mapper.WebApplicationMapperException

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class ChannelServiceTest {
    private val account = AccessToken(tokenId = 10, resourceOwnerId = 20, accountId = 30, scopes = "read", permissions = 0)

    @Mock
    private lateinit var settingService: SettingService

    @Mock
    private lateinit var listService: ListService

    @InjectMocks
    private lateinit var channelService: ChannelService

    @Test
    fun `missing streams are rejected`() {
        val err: WebApplicationMapperException = assertThrows { channelService.channelNameToIds(account, null, StreamParams()) }

        assertEquals(400, err.status)
        assertEquals("Unknown stream type", err.message)
    }

    @Test
    fun `the system stream cannot be requested`() {
        assertEquals(400, assertThrows<WebApplicationMapperException> { channelService.channelNameToIds(account, ChannelName.SYSTEM, StreamParams()) }.status)
    }

    @Test
    fun `the user stream includes notifications for tokens that can read them`() {
        val resolution = channelService.channelNameToIds(account, ChannelName.USER, StreamParams())

        assertEquals(listOf("timeline:30", "timeline:30:notifications"), resolution.channelIds)
        assertFalse(resolution.needsFiltering)
        verifyNoInteractions(settingService)
    }

    @Test
    fun `the user stream excludes notifications for tokens that can only read statuses`() {
        assertEquals(listOf("timeline:30"), channelService.channelNameToIds(account.copy(scopes = "read:statuses"), ChannelName.USER, StreamParams()).channelIds)
    }

    @ParameterizedTest
    @CsvSource(
        "USER_NOTIFICATION, timeline:30:notifications",
        "DIRECT, timeline:direct:30",
    )
    fun personalStreamsAreNotFiltered(channel: ChannelName, channelId: String) {
        val resolution = channelService.channelNameToIds(account, channel, StreamParams())

        assertEquals(listOf(channelId), resolution.channelIds)
        assertFalse(resolution.needsFiltering)
    }

    @ParameterizedTest
    @CsvSource(
        "PUBLIC, timeline:public",
        "PUBLIC_MEDIA, timeline:public:media",
        "PUBLIC_LOCAL, timeline:public:local",
        "PUBLIC_LOCAL_MEDIA, timeline:public:local:media",
        "PUBLIC_REMOTE, timeline:public:remote",
        "PUBLIC_REMOTE_MEDIA, timeline:public:remote:media",
    )
    fun publicStreamsAreFiltered(channel: ChannelName, channelId: String) {
        val resolution = channelService.channelNameToIds(account, channel, StreamParams())

        assertEquals(listOf(channelId), resolution.channelIds)
        assertTrue(resolution.needsFiltering)
    }

    @Test
    fun `hashtag streams use the normalized tag`() {
        val params = StreamParams(tag = "#Café")
        val resolution = channelService.channelNameToIds(account, ChannelName.HASHTAG, params)

        assertEquals(listOf("timeline:hashtag:cafe"), resolution.channelIds)
        assertEquals(listOf("hashtag", "#Café"), resolution.streamName)
        assertEquals(params, resolution.params)
        assertTrue(resolution.needsFiltering)
    }

    @Test
    fun `local hashtag streams use the local channel of the tag`() {
        val resolution = channelService.channelNameToIds(account, ChannelName.HASHTAG_LOCAL, StreamParams(tag = "Kotlin"))

        assertEquals(listOf("timeline:hashtag:kotlin:local"), resolution.channelIds)
    }

    @ParameterizedTest
    @NullAndEmptySource
    fun hashtagStreamsRequireTag(tag: String?) {
        val err: WebApplicationMapperException = assertThrows { channelService.channelNameToIds(account, ChannelName.HASHTAG, StreamParams(tag = tag)) }

        assertEquals(400, err.status)
        assertEquals("Missing tag name parameter", err.message)
    }

    @Test
    fun `list streams are available to the owner of the list`() {
        whenever(listService.isOwnedBy(5, 30)).thenReturn(true)

        val resolution = channelService.channelNameToIds(account, ChannelName.LIST, StreamParams(list = "5"))

        assertEquals(listOf("timeline:list:5"), resolution.channelIds)
        assertFalse(resolution.needsFiltering)
    }

    @Test
    fun `list streams of other accounts are rejected`() {
        whenever(listService.isOwnedBy(5, 30)).thenReturn(false)

        val err: WebApplicationMapperException = assertThrows { channelService.channelNameToIds(account, ChannelName.LIST, StreamParams(list = "5")) }

        assertEquals(401, err.status)
        assertEquals("Not authorized to stream this list", err.message)
    }

    @Test
    fun `list streams with an invalid id are rejected without querying the database`() {
        assertEquals(401, assertThrows<WebApplicationMapperException> { channelService.channelNameToIds(account, ChannelName.LIST, StreamParams(list = "five")) }.status)
        verifyNoInteractions(listService)
    }

    @ParameterizedTest
    @NullAndEmptySource
    fun listStreamsRequireList(list: String?) {
        val err: WebApplicationMapperException = assertThrows { channelService.channelNameToIds(account, ChannelName.LIST, StreamParams(list = list)) }

        assertEquals(400, err.status)
        assertEquals("Missing list name parameter", err.message)
    }

    @ParameterizedTest
    @ValueSource(strings = ["PUBLIC", "HASHTAG"])
    fun accountsThatCanViewFeedsIgnoreFeedSettings(channel: ChannelName) {
        val resolution = channelService.channelNameToIds(account.copy(permissions = Permissions.VIEW_FEEDS), channel, StreamParams(tag = "kotlin"))

        assertTrue(resolution.needsFiltering)
        assertFalse(resolution.filterLocal)
        assertFalse(resolution.filterRemote)
        verifyNoInteractions(settingService)
    }

    @Test
    fun `public streams drop local statuses when the local live feed is disabled`() {
        whenever(settingService.findByKind("public")).thenReturn(listOf(
            FeedSetting("local_live_feed_access", "--- disabled\n"),
            FeedSetting("remote_live_feed_access", "--- public\n"),
        ))

        val resolution = channelService.channelNameToIds(account, ChannelName.PUBLIC, StreamParams())

        assertTrue(resolution.filterLocal)
        assertFalse(resolution.filterRemote)
    }

    @Test
    fun `hashtag streams drop remote statuses when the remote topic feed is disabled`() {
        whenever(settingService.findByKind("hashtag")).thenReturn(listOf(
            FeedSetting("local_topic_feed_access", null),
            FeedSetting("remote_topic_feed_access", "--- disabled\n"),
        ))

        val resolution = channelService.channelNameToIds(account, ChannelName.HASHTAG_LOCAL, StreamParams(tag = "kotlin"))

        assertFalse(resolution.filterLocal)
        assertTrue(resolution.filterRemote)
    }

    @Test
    fun `nothing is dropped without feed settings`() {
        val resolution = channelService.channelNameToIds(account, ChannelName.PUBLIC_LOCAL, StreamParams())

        assertFalse(resolution.filterLocal)
        assertFalse(resolution.filterRemote)
    }
}
