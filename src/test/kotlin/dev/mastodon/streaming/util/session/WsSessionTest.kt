package dev.mastodon.streaming.util.session

import dev.mastodon.streaming.dto.AccessToken
import dev.mastodon.streaming.dto.ErrorResponse
import dev.mastodon.streaming.dto.StreamMessage

import io.quarkus.websockets.next.WebSocketConnection
import io.smallrye.mutiny.Uni
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class WsSessionTest {
    private val account = AccessToken(tokenId = 10, resourceOwnerId = 20, accountId = 30, scopes = "read", permissions = 0)

    @Mock
    private lateinit var ws: WebSocketConnection

    @Test
    fun `messages are sent with their stream name and event`() {
        val message = StreamMessage(listOf("hashtag", "kotlin"), "update", """{"id":"1"}""")

        whenever(ws.isOpen).thenReturn(true)
        whenever(ws.sendText(message)).thenReturn(Uni.createFrom().voidItem())

        WsSession(ws, account).send(listOf("hashtag", "kotlin"), "update", """{"id":"1"}""")

        verify(ws).sendText(message)
    }

    @Test
    fun `custom messages are sent as they are`() {
        whenever(ws.isOpen).thenReturn(true)
        whenever(ws.sendText(ErrorResponse("Error subscribing from channel"))).thenReturn(Uni.createFrom().voidItem())

        WsSession(ws, account).sendCustom(ErrorResponse("Error subscribing from channel"))

        verify(ws).sendText(ErrorResponse("Error subscribing from channel"))
    }

    @Test
    fun `nothing is written to a closed socket`() {
        whenever(ws.isOpen).thenReturn(false)

        WsSession(ws, account).send(listOf("public"), "delete", "1")

        verify(ws, never()).sendText(any<Any>())
    }

    @Test
    fun `failures to send are not thrown`() {
        whenever(ws.isOpen).thenReturn(true)
        whenever(ws.sendText(any<Any>())).thenReturn(Uni.createFrom().failure(IllegalStateException("Connection reset")))

        WsSession(ws, account).send(listOf("public"), "delete", "1")
    }

    @Test
    fun `closing the session closes the socket`() {
        whenever(ws.close()).thenReturn(Uni.createFrom().voidItem())

        WsSession(ws, account).close()

        verify(ws).close()
    }

    @Test
    fun `websocket sessions are counted as websocket connections`() {
        assertEquals(ConnectionType.WEBSOCKET, WsSession(ws, account).type)
    }
}
