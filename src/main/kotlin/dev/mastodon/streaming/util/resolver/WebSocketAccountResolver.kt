package dev.mastodon.streaming.util.resolver

import dev.mastodon.streaming.db.token.AccessTokenService
import dev.mastodon.streaming.dto.AccessToken
import io.netty.handler.codec.http.QueryStringDecoder
import io.quarkus.websockets.next.WebSocketConnection
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import kotlin.jvm.Throws

@ApplicationScoped
class WebSocketAccountResolver {
    @Inject
    private lateinit var ws: WebSocketConnection

    @Inject
    private lateinit var tokenService: AccessTokenService

    @Throws(IllegalStateException::class)
    fun resolveToken(): String {
        val header = ws.handshakeRequest()
            .header("Authorization")
            ?.removePrefix("Bearer ")
            ?.takeIf { it.isNotBlank() } ?: ws.handshakeRequest().header("Sec-WebSocket-Protocol")
        val accessToken = ws.handshakeRequest()
            .query()
            .let { QueryStringDecoder("/?$it") }
            .parameters()
            ?.get("access_token")
            ?.firstOrNull()

        return header ?: accessToken ?: throw IllegalStateException("No token found")
    }
    
    @Throws(IllegalStateException::class)
    fun getAccountId(): AccessToken =
        tokenService.findTokenDetailsDto(this.resolveToken()) ?: throw IllegalStateException("No account found")
}
