package dev.mastodon.streaming

import dev.mastodon.streaming.db.token.AccessTokenService
import dev.mastodon.streaming.dto.AccessToken
import dev.mastodon.streaming.util.resolver.WebSocketAccountResolver

import io.quarkus.test.junit.QuarkusTest
import io.quarkus.websockets.next.HandshakeRequest
import io.quarkus.websockets.next.WebSocketConnection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

@QuarkusTest
class WebSocketAccountResolverTest {
    @Mock
    lateinit var ws: WebSocketConnection

    @Mock
    lateinit var tokenService: AccessTokenService

    @Mock
    lateinit var handshake: HandshakeRequest

    @InjectMocks
    lateinit var resolver: WebSocketAccountResolver

    init {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `should resolve token from Authorization header`() {
        `when`(ws.handshakeRequest()).thenReturn(handshake)
        `when`(handshake.header("Authorization")).thenReturn("Bearer bearer-token")
        `when`(handshake.header("Sec-WebSocket-Protocol")).thenReturn(null)
        `when`(handshake.query()).thenReturn("")

        val token = resolver.resolveToken()
        assertEquals("bearer-token", token)
    }

    @Test
    fun `should resolve token from Sec-WebSocket-Protocol header`() {
        `when`(ws.handshakeRequest()).thenReturn(handshake)
        `when`(handshake.header("Authorization")).thenReturn(null)
        `when`(handshake.header("Sec-WebSocket-Protocol")).thenReturn("sec.websocket.protocol-token")
        `when`(handshake.query()).thenReturn("")

        val token = resolver.resolveToken()
        assertEquals("sec.websocket.protocol-token", token)
    }

    @Test
    fun `should resolve token from query parameter`() {
        `when`(ws.handshakeRequest()).thenReturn(handshake)
        `when`(handshake.header("Authorization")).thenReturn(null)
        `when`(handshake.header("Sec-WebSocket-Protocol")).thenReturn(null)
        `when`(handshake.query()).thenReturn("access_token=query-token")

        val token = resolver.resolveToken()
        assertEquals("query-token", token)
    }

    @Test
    fun `should throw exception when no token exists`() {
        `when`(ws.handshakeRequest()).thenReturn(handshake)
        `when`(handshake.header("Authorization")).thenReturn(null)
        `when`(handshake.header("Sec-WebSocket-Protocol")).thenReturn(null)
        `when`(handshake.query()).thenReturn("")

        assertThrows(IllegalStateException::class.java) {
            resolver.resolveToken()
        }
    }

    @Test
    fun `getAccountId should return AccessToken when service finds token`() {
        val accessToken = AccessToken(
            tokenId = 1,
            resourceOwnerId = 2,
            accountId = 3,
            chosenLanguages = listOf("en"),
            scopes = "scope",
            permissions = 1
        )

        `when`(ws.handshakeRequest()).thenReturn(handshake)
        `when`(handshake.header("Authorization")).thenReturn("Bearer bearer-token")
        `when`(handshake.query()).thenReturn("")
        `when`(tokenService.findTokenDetailsDto("bearer-token")).thenReturn(accessToken)

        val result = resolver.getAccountId()
        assertEquals(accessToken, result)
    }

    @Test
    fun `getAccountId should throw when token service returns null`() {
        `when`(ws.handshakeRequest()).thenReturn(handshake)
        `when`(handshake.header("Authorization")).thenReturn("Bearer bearer-token")
        `when`(handshake.query()).thenReturn("")
        `when`(tokenService.findTokenDetailsDto("bearer-token")).thenReturn(null)

        assertThrows(IllegalStateException::class.java) {
            resolver.getAccountId()
        }
    }
}
