package dev.mastodon.streaming.util.resolver

import dev.mastodon.streaming.util.mapper.toErrorResponse
import io.quarkus.logging.Log
import io.quarkus.vertx.VertxContextSupport
import io.quarkus.websockets.next.HttpUpgradeCheck
import io.quarkus.websockets.next.HttpUpgradeCheck.CheckResult
import io.quarkus.websockets.next.HttpUpgradeCheck.HttpUpgradeContext
import io.quarkus.websockets.next.UserData
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

/**
 * Authenticates websocket clients before the connection is upgraded, a client
 * without a valid access token receives a plain HTTP error response instead.
 * The account stays attached to the connection through its user data.
 */
@ApplicationScoped
class WebSocketHttpUpgradeAuth : HttpUpgradeCheck {
    @Inject
    private lateinit var accountResolver: AccountResolver

    override fun perform(context: HttpUpgradeContext): Uni<CheckResult> =
        VertxContextSupport.executeBlocking { accountResolver.accountFromRequest(context.httpRequest()) }
            .map { account ->
                context.userData().put(UserData.TypedKey("account"), account)

                CheckResult.permitUpgradeSync()
            }
            .onFailure()
            .recoverWithItem { err -> reject(err) }

    private fun reject(err: Throwable): CheckResult {
        Log.error("WebSocket upgrade rejected", err)
        
        return CheckResult.rejectUpgradeSync(err.toErrorResponse().status, mapOf(
            "Connection" to listOf("close"),
            "Content-Type" to listOf("text/plain"),
            "X-Error-Message" to listOf(err.toErrorResponse().error),
        ))
    }
}
