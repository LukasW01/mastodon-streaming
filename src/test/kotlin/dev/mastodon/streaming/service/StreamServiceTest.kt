package dev.mastodon.streaming.service

import dev.mastodon.streaming.FakeSession
import dev.mastodon.streaming.SentMessage
import dev.mastodon.streaming.dto.AccessToken
import dev.mastodon.streaming.dto.ChannelResolution
import dev.mastodon.streaming.dto.RedisMessage
import dev.mastodon.streaming.dto.StreamParams
import dev.mastodon.streaming.jsonMapper
import dev.mastodon.streaming.prometheus.Metrics
import dev.mastodon.streaming.util.ChannelName
import dev.mastodon.streaming.util.session.ConnectionType
import dev.mastodon.streaming.withField

import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.TextNode
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

import java.util.concurrent.ExecutorService

@ExtendWith(MockitoExtension::class)
class StreamServiceTest {
    private val session = FakeSession(AccessToken(tokenId = 10, resourceOwnerId = 20, accountId = 30, scopes = "read", permissions = 0))
    private val stopHeartbeat: () -> Unit = mock()
    private val status = jsonMapper().readTree("""{"id":"1"}""")
    private val publicStream = ChannelResolution(ChannelName.PUBLIC, StreamParams(), listOf("timeline:public"), needsFiltering = true)
    private val hashtagStream = ChannelResolution(ChannelName.HASHTAG, StreamParams(tag = "kotlin"), listOf("timeline:hashtag:kotlin"), needsFiltering = false)

    @Spy
    private val metrics: Metrics = Metrics().withField("registry", SimpleMeterRegistry())

    @Mock
    private lateinit var redisService: RedisService

    @Mock
    private lateinit var statusFilterService: StatusFilterService

    @Mock
    private lateinit var executor: ExecutorService

    @InjectMocks
    private lateinit var streamService: StreamService

    private fun subscribe(resolution: ChannelResolution): (RedisMessage) -> Unit {
        doReturn(stopHeartbeat).whenever(redisService).subscriptionHeartbeat(resolution.channelIds)

        streamService.subscribeToChannel(session, resolution)

        return session.subscriptions.getValue(resolution.channelIds.joinToString(";")).listener
    }

    private fun filterDirectly() {
        doAnswer { it.getArgument<Runnable>(0).run() }.whenever(executor).execute(any())
    }

    private fun connectedChannels(channel: ChannelName): Long = metrics.connectedChannels(ConnectionType.EVENTSOURCE, channel).get()

    @Test
    fun `opening a session subscribes it to its system channels`() {
        streamService.open(session)

        verify(redisService).subscribe(eq("timeline:access_token:10"), any())
        verify(redisService).subscribe(eq("timeline:system:30"), any())
        assertEquals(setOf("timeline:access_token:10", "timeline:system:30"), session.subscriptions.keys)
        assertEquals(1, metrics.connectedClients(ConnectionType.EVENTSOURCE).get())
        assertEquals(2, connectedChannels(ChannelName.SYSTEM))
    }

    @Test
    fun `kill events on the system channel close the session`() {
        streamService.open(session)

        session.subscriptions.getValue("timeline:system:30").listener(RedisMessage("kill"))

        assertTrue(session.closed)
    }

    @Test
    fun `other events on the system channel are ignored`() {
        streamService.open(session)

        session.subscriptions.getValue("timeline:access_token:10").listener(RedisMessage("filters_changed"))

        assertFalse(session.closed)
        assertTrue(session.sent.isEmpty())
    }

    @Test
    fun `subscribing listens to all channels of the stream`() {
        val resolution = ChannelResolution(ChannelName.USER, StreamParams(), listOf("timeline:30", "timeline:30:notifications"), needsFiltering = false)

        val listener = subscribe(resolution)

        verify(redisService).subscribe("timeline:30", listener)
        verify(redisService).subscribe("timeline:30:notifications", listener)
        assertEquals(ChannelName.USER, session.subscriptions.getValue("timeline:30;timeline:30:notifications").channelName)
        assertEquals(1, connectedChannels(ChannelName.USER))
    }

    @Test
    fun `subscribing to a stream twice is ignored`() {
        subscribe(hashtagStream)
        streamService.subscribeToChannel(session, hashtagStream)

        verify(redisService, times(1)).subscriptionHeartbeat(hashtagStream.channelIds)
        verify(redisService, times(1)).subscribe(eq("timeline:hashtag:kotlin"), any())
        assertEquals(1, connectedChannels(ChannelName.HASHTAG))
    }

    @Test
    fun `messages of unfiltered streams are transmitted directly`() {
        subscribe(hashtagStream)(RedisMessage("update", status))

        assertEquals(listOf(SentMessage(listOf("hashtag", "kotlin"), "update", """{"id":"1"}""")), session.sent)
        assertEquals(1.0, metrics.messagesSent(ConnectionType.EVENTSOURCE).count())
        verifyNoInteractions(statusFilterService, executor)
    }

    @Test
    fun `messages without event or payload are dropped`() {
        val listener = subscribe(hashtagStream)

        listener(RedisMessage(null, status))
        listener(RedisMessage("", status))
        listener(RedisMessage("update", null))
        listener(RedisMessage("update", NullNode.instance))

        assertTrue(session.sent.isEmpty())
    }

    @ParameterizedTest
    @ValueSource(strings = ["update", "status.update"])
    fun statusEventsOfFilteredStreamsAreTransmittedAfterFiltering(event: String) {
        filterDirectly()
        val message = RedisMessage(event, status)
        whenever(statusFilterService.filter(session, publicStream, message)).thenReturn("""{"id":"1","filtered":[]}""")

        subscribe(publicStream)(message)

        assertEquals(listOf(SentMessage(listOf("public"), event, """{"id":"1","filtered":[]}""")), session.sent)
    }

    @Test
    fun `statuses removed by the filter are not transmitted`() {
        filterDirectly()
        val message = RedisMessage("update", status)
        whenever(statusFilterService.filter(session, publicStream, message)).thenReturn(null)

        subscribe(publicStream)(message)

        assertTrue(session.sent.isEmpty())
        assertEquals(0.0, metrics.messagesSent(ConnectionType.EVENTSOURCE).count())
    }

    @Test
    fun `errors while filtering do not reach the redis connection`() {
        filterDirectly()
        val message = RedisMessage("update", status)
        whenever(statusFilterService.filter(session, publicStream, message)).thenThrow(IllegalStateException("Database unavailable"))

        subscribe(publicStream)(message)

        assertTrue(session.sent.isEmpty())
    }

    @Test
    fun `other events of filtered streams are transmitted directly`() {
        subscribe(publicStream)(RedisMessage("delete", TextNode("1")))

        assertEquals(listOf(SentMessage(listOf("public"), "delete", "1")), session.sent)
        verifyNoInteractions(statusFilterService, executor)
    }

    @Test
    fun `removing a subscription stops listening to its channels`() {
        val listener = subscribe(publicStream)

        streamService.removeSubscription(session, publicStream.channelIds)

        verify(redisService).unsubscribe("timeline:public", listener)
        verify(stopHeartbeat).invoke()
        assertTrue(session.subscriptions.isEmpty())
        assertEquals(0, connectedChannels(ChannelName.PUBLIC))
    }

    @Test
    fun `removing an unknown subscription is ignored`() {
        streamService.removeSubscription(session, listOf("timeline:public"))

        verify(redisService, never()).unsubscribe(any(), any())
    }

    @Test
    fun `closing a session removes all of its subscriptions`() {
        streamService.open(session)

        val listener = subscribe(publicStream)

        streamService.close(session)

        verify(redisService).unsubscribe(eq("timeline:access_token:10"), any())
        verify(redisService).unsubscribe(eq("timeline:system:30"), any())
        verify(redisService).unsubscribe("timeline:public", listener)
        verify(stopHeartbeat).invoke()
        assertTrue(session.subscriptions.isEmpty())
        assertEquals(0, metrics.connectedClients(ConnectionType.EVENTSOURCE).get())
        assertEquals(0, connectedChannels(ChannelName.SYSTEM))
        assertEquals(0, connectedChannels(ChannelName.PUBLIC))
    }
}
