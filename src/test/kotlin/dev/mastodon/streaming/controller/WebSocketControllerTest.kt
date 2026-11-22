package dev.mastodon.streaming.controller

import dev.mastodon.streaming.dto.AccessToken
import dev.mastodon.streaming.dto.ChannelResolution
import dev.mastodon.streaming.dto.ErrorResponse
import dev.mastodon.streaming.dto.StreamParams
import dev.mastodon.streaming.dto.Subscribe
import dev.mastodon.streaming.service.ChannelService
import dev.mastodon.streaming.service.StreamService
import dev.mastodon.streaming.util.ChannelName
import dev.mastodon.streaming.util.mapper.WebApplicationMapperException
import dev.mastodon.streaming.util.session.WsSession

import io.quarkus.websockets.next.HandshakeRequest
import io.quarkus.websockets.next.TextDecodeException
import io.quarkus.websockets.next.UserData
import io.quarkus.websockets.next.WebSocketConnection
import io.smallrye.mutiny.Uni
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class WebSocketControllerTest {
    private val account = AccessToken(tokenId = 10, resourceOwnerId = 20, accountId = 30, scopes = "read", permissions = 0)
    private val sessionKey: UserData.TypedKey<WsSession> = UserData.TypedKey("session")
    private val hashtagStream = ChannelResolution(ChannelName.HASHTAG, StreamParams(tag = "kotlin"), listOf("timeline:hashtag:kotlin"), needsFiltering = true)

    @Mock
    private lateinit var channelService: ChannelService

    @Mock
    private lateinit var streamService: StreamService

    @Mock
    private lateinit var ws: WebSocketConnection

    @Mock
    private lateinit var userData: UserData

    @Mock
    private lateinit var handshakeRequest: HandshakeRequest

    @InjectMocks
    private lateinit var controller: WebSocketController

    @BeforeEach
    fun setUp() {
        whenever(ws.userData()).thenReturn(userData)
    }

    private fun open(query: String?): WsSession {
        whenever(userData.get(UserData.TypedKey<AccessToken>("account"))).thenReturn(account)
        whenever(ws.handshakeRequest()).thenReturn(handshakeRequest)
        whenever(handshakeRequest.query()).thenReturn(query)

        controller.onOpen(ws)

        return argumentCaptor<WsSession>().apply { verify(userData).put(eq(sessionKey), capture()) }.firstValue
    }

    private fun session(account: AccessToken = this.account): WsSession = WsSession(ws, account).also { session ->
        whenever(userData.get(sessionKey)).thenReturn(session)
    }

    private fun expectError(error: String) {
        whenever(ws.isOpen).thenReturn(true)
        whenever(ws.sendText(ErrorResponse(error))).thenReturn(Uni.createFrom().voidItem())
    }

    @Test
    fun `opening a connection starts a session for the authenticated account`() {
        val session = open(null)

        assertEquals(account, session.account)
        verify(streamService).open(session)
        verifyNoInteractions(channelService)
    }

    @Test
    fun `the stream of the connection url is subscribed to right away`() {
        whenever(channelService.channelNameToIds(account, ChannelName.HASHTAG, StreamParams(tag = "kotlin"))).thenReturn(hashtagStream)

        val session = open("stream=hashtag&tag=kotlin")

        verify(streamService).subscribeToChannel(session, hashtagStream)
    }

    @Test
    fun `an empty stream in the connection url is ignored`() {
        open("stream=&tag=kotlin")

        verifyNoInteractions(channelService)
    }

    @Test
    fun `subscribe messages subscribe the session to the stream`() {
        val session = session()
        val listStream = ChannelResolution(ChannelName.LIST, StreamParams(list = "5"), listOf("timeline:list:5"), needsFiltering = false)
        whenever(channelService.channelNameToIds(account, ChannelName.LIST, StreamParams(list = "5"))).thenReturn(listStream)

        controller.onMessage(ws, Subscribe(type = "subscribe", stream = listOf("list"), list = "5"))

        verify(streamService).subscribeToChannel(session, listStream)
    }

    @Test
    fun `subscribing without the required scopes is reported to the client`() {
        session(account.copy(scopes = "read:statuses"))
        expectError("Error subscribing from channel")

        controller.onMessage(ws, Subscribe(type = "subscribe", stream = listOf("user:notification")))

        verify(ws).sendText(ErrorResponse("Error subscribing from channel"))
        verifyNoInteractions(channelService, streamService)
    }

    @Test
    fun `subscribing to an unknown stream is reported to the client`() {
        session()
        expectError("Error subscribing from channel")
        whenever(channelService.channelNameToIds(account, null, StreamParams())).thenThrow(WebApplicationMapperException("Unknown stream type", 400))

        controller.onMessage(ws, Subscribe(type = "subscribe", stream = listOf("nonsense")))

        verify(ws).sendText(ErrorResponse("Error subscribing from channel"))
        verifyNoInteractions(streamService)
    }

    @Test
    fun `unsubscribe messages remove the subscription`() {
        val session = session()
        whenever(channelService.channelNameToIds(account, ChannelName.HASHTAG, StreamParams(tag = "kotlin"))).thenReturn(hashtagStream)

        controller.onMessage(ws, Subscribe(type = "unsubscribe", stream = listOf("hashtag"), tag = "kotlin"))

        verify(streamService).removeSubscription(session, listOf("timeline:hashtag:kotlin"))
    }

    @Test
    fun `unsubscribe errors are reported to the client`() {
        session()
        expectError("Error unsubscribing from channel")
        val err = WebApplicationMapperException("Missing tag name parameter", 400)
        whenever(channelService.channelNameToIds(account, ChannelName.HASHTAG, StreamParams())).thenThrow(err)

        controller.onMessage(ws, Subscribe(type = "unsubscribe", stream = listOf("hashtag")))

        verify(ws).sendText(ErrorResponse("Error unsubscribing from channel"))
    }

    @Test
    fun `unknown message types are ignored`() {
        session()

        controller.onMessage(ws, Subscribe(type = "ping", stream = listOf("public")))

        verifyNoInteractions(channelService, streamService)
    }

    @Test
    fun `closing the connection closes the session`() {
        val session = session()

        controller.onClose(ws)

        verify(streamService).close(session)
    }

    @Test
    fun `messages that cannot be decoded keep the connection open`() {
        session()

        controller.onDecodeError(ws, TextDecodeException("not json", "Unable to decode"))

        verify(ws, never()).close()
        verify(ws, never()).sendText(any<Any>())
    }
}
