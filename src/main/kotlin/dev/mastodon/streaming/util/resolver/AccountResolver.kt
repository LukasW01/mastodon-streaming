package dev.mastodon.streaming.util.resolver

import dev.mastodon.streaming.db.token.AccessTokenService
import dev.mastodon.streaming.dto.AccessToken
import dev.mastodon.streaming.util.mapper.WebApplicationMapperException
import io.vertx.core.http.HttpServerRequest
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.context.control.ActivateRequestContext
import jakarta.inject.Inject
import jakarta.ws.rs.core.Response

/** Resolves the account of an eventsource request or a websocket upgrade request from its access token. */
@ApplicationScoped
class AccountResolver {
    @Inject
    private lateinit var tokenService: AccessTokenService

    @ActivateRequestContext
    fun accountFromRequest(request: HttpServerRequest): AccessToken {
        val authorization = request.getHeader("Authorization")
        val accessToken = request.getParam("access_token")?.takeIf { it.isNotEmpty() } ?: request.getHeader("Sec-WebSocket-Protocol")

        if (authorization.isNullOrEmpty() && accessToken.isNullOrEmpty()) {
            throw WebApplicationMapperException("Missing access token", Response.Status.UNAUTHORIZED.statusCode)
        }

        val token = if (authorization.isNullOrEmpty()) accessToken else authorization.removePrefix("Bearer ")

        return tokenService.findTokenDetailsDto(token) ?: throw WebApplicationMapperException("Invalid access token", Response.Status.UNAUTHORIZED.statusCode)
    }
}
