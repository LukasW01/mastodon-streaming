package dev.mastodon.streaming.util.resolver

import dev.mastodon.streaming.db.token.AccessTokenService
import dev.mastodon.streaming.dto.AccessToken
import dev.mastodon.streaming.util.mapper.WebApplicationMapperException

import io.vertx.core.http.HttpServerRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class AccountResolverTest {
    private val account = AccessToken(tokenId = 10, resourceOwnerId = 20, accountId = 30, scopes = "read", permissions = 0)

    @Mock
    private lateinit var tokenService: AccessTokenService

    @Mock
    private lateinit var request: HttpServerRequest

    @InjectMocks
    private lateinit var accountResolver: AccountResolver

    @Test
    fun `the token is read from the authorization header`() {
        whenever(request.getHeader("Authorization")).thenReturn("Bearer secret")
        whenever(tokenService.findTokenDetailsDto("secret")).thenReturn(account)

        assertEquals(account, accountResolver.accountFromRequest(request))
    }

    @Test
    fun `the token is read from the access token parameter`() {
        whenever(request.getParam("access_token")).thenReturn("secret")
        whenever(tokenService.findTokenDetailsDto("secret")).thenReturn(account)

        assertEquals(account, accountResolver.accountFromRequest(request))
    }

    @Test
    fun `the token is read from the websocket subprotocol`() {
        whenever(request.getHeader("Authorization")).thenReturn(null)
        whenever(request.getParam("access_token")).thenReturn("")
        whenever(request.getHeader("Sec-WebSocket-Protocol")).thenReturn("secret")
        whenever(tokenService.findTokenDetailsDto("secret")).thenReturn(account)

        assertEquals(account, accountResolver.accountFromRequest(request))
    }

    @Test
    fun `the authorization header takes precedence`() {
        whenever(request.getHeader("Authorization")).thenReturn("Bearer header")
        whenever(request.getParam("access_token")).thenReturn("parameter")
        whenever(tokenService.findTokenDetailsDto("header")).thenReturn(account)

        assertEquals(account, accountResolver.accountFromRequest(request))
    }

    @Test
    fun `requests without token are rejected`() {
        val err: WebApplicationMapperException = assertThrows { accountResolver.accountFromRequest(request) }

        assertEquals(401, err.status)
        assertEquals("Missing access token", err.message)
        verifyNoInteractions(tokenService)
    }

    @Test
    fun `unknown tokens are rejected`() {
        whenever(request.getParam("access_token")).thenReturn("revoked")

        val err: WebApplicationMapperException = assertThrows { accountResolver.accountFromRequest(request) }

        assertEquals(401, err.status)
        assertEquals("Invalid access token", err.message)
    }
}
