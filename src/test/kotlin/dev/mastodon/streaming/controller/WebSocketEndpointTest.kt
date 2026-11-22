package dev.mastodon.streaming.controller

import dev.mastodon.streaming.db.token.AccessTokenService
import dev.mastodon.streaming.dto.AccessToken
import dev.mastodon.streaming.jsonMapper
import dev.mastodon.streaming.service.StreamService
import dev.mastodon.streaming.util.ChannelName
import dev.mastodon.streaming.util.session.ConnectionType

import io.quarkus.test.InjectMock
import io.quarkus.test.common.http.TestHTTPResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.mockito.InjectSpy
import io.quarkus.websockets.next.BasicWebSocketConnector
import io.quarkus.websockets.next.WebSocketClientConnection
import io.vertx.core.http.UpgradeRejectedException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.never
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

import java.net.URI
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@QuarkusTest
class WebSocketEndpointTest {
    private val token = AccessToken(tokenId = 10, resourceOwnerId = 20, accountId = 30, scopes = "read", permissions = 0)
    private val messages: LinkedBlockingQueue<String> = LinkedBlockingQueue()
    private val connections: MutableList<WebSocketClientConnection> = mutableListOf()

    @InjectMock
    lateinit var accessTokenService: AccessTokenService

    @InjectSpy
    lateinit var streamService: StreamService

    @TestHTTPResource("/")
    lateinit var baseUri: URI

    @AfterEach
    fun tearDown() {
        connections.filter { it.isOpen }.forEach { it.closeAndAwait() }
    }

    private fun connector(path: String = "/api/v1/streaming"): BasicWebSocketConnector = BasicWebSocketConnector.create()
        .baseUri(baseUri)
        .path(path)
        .onTextMessage { _, message -> messages.add(message) }

    private fun BasicWebSocketConnector.open(): WebSocketClientConnection = connectAndAwait().also { connections += it }

    private fun nextMessage(): String? = messages.poll(10, TimeUnit.SECONDS)

    @Test
    fun `connections without access token are rejected`() {
        val err: Exception = assertThrows { connector().open() }

        assertEquals(401, generateSequence<Throwable>(err) { it.cause }.filterIsInstance<UpgradeRejectedException>().first().status)
        verify(streamService, never()).open(any())
    }

    @Test
    fun `connections with an unknown access token are rejected`() {
        val err: Exception = assertThrows { connector().addHeader("Authorization", "Bearer revoked").open() }

        assertEquals(401, generateSequence<Throwable>(err) { it.cause }.filterIsInstance<UpgradeRejectedException>().first().status)
    }

    @Test
    fun `the access token can be sent as subprotocol`() {
        whenever(accessTokenService.findTokenDetailsDto("secret")).thenReturn(token)

        val connection = connector().addSubprotocol("secret").open()

        assertTrue(connection.isOpen)
        verify(streamService, timeout(10_000)).open(argThat { type == ConnectionType.WEBSOCKET && account == token })
    }

    @Test
    fun `the stream of the connection url is subscribed to on connect`() {
        whenever(accessTokenService.findTokenDetailsDto("secret")).thenReturn(token)

        connector("/api/v1/streaming?access_token=secret&stream=public:local").open()

        verify(streamService, timeout(10_000)).subscribeToChannel(any(), argThat { channelName == ChannelName.PUBLIC_LOCAL })
    }

    @Test
    fun `clients can subscribe and the session is closed on disconnect`() {
        whenever(accessTokenService.findTokenDetailsDto("secret")).thenReturn(token)
        val connection = connector().addHeader("Authorization", "Bearer secret").open()

        connection.sendTextAndAwait("""{"type": "subscribe", "stream": "hashtag", "tag": "Kotlin"}""")
        verify(streamService, timeout(10_000)).subscribeToChannel(any(), argThat { channelIds == listOf("timeline:hashtag:kotlin") })

        connection.closeAndAwait()
        verify(streamService, timeout(10_000)).close(any())
    }

    @Test
    fun `subscription errors are sent to the client`() {
        whenever(accessTokenService.findTokenDetailsDto("secret")).thenReturn(token)
        val connection = connector().addHeader("Authorization", "Bearer secret").open()

        connection.sendTextAndAwait("""{"type": "subscribe", "stream": "hashtag"}""")

        assertEquals(jsonMapper().readTree("""{"error": "Error subscribing from channel"}"""), jsonMapper().readTree(nextMessage()))
    }

    @Test
    fun `messages that cannot be decoded do not close the connection`() {
        whenever(accessTokenService.findTokenDetailsDto("secret")).thenReturn(token)
        val connection = connector().addHeader("Authorization", "Bearer secret").open()

        connection.sendTextAndAwait("not json")
        connection.sendTextAndAwait("""{"type": "subscribe", "stream": "hashtag"}""")

        assertEquals(jsonMapper().readTree("""{"error": "Error subscribing from channel"}"""), jsonMapper().readTree(nextMessage()))
        assertTrue(connection.isOpen)
    }
}
