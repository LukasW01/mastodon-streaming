package dev.mastodon.streaming.controller

import dev.mastodon.streaming.dto.Subscribe
import dev.mastodon.streaming.util.ChannelName
import dev.mastodon.streaming.util.resolver.WebSocketAccountResolver
import io.quarkus.logging.Log
import io.quarkus.websockets.next.CloseReason
import io.quarkus.websockets.next.OnOpen
import io.quarkus.websockets.next.OnTextMessage
import io.quarkus.websockets.next.WebSocket
import io.quarkus.websockets.next.WebSocketConnection
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.context.control.ActivateRequestContext
import jakarta.inject.Inject

@ApplicationScoped
@WebSocket(path = "/api/v1/streaming")
class MastodonWebSocketController {
    @Inject
    private lateinit var ws: WebSocketConnection

    @Inject
    private lateinit var account: WebSocketAccountResolver
    
    @OnOpen
    @ActivateRequestContext
    fun onOpen() {
        try {
            account.getAccountId(account.resolveToken())
        } catch (e: Exception) {
            Log.info("WebSocket connection error: ${e.message}")
            ws.close(CloseReason(CloseReason.NORMAL.code, "Unauthorized")).await().indefinitely()
        }
    }
    
    @OnTextMessage
    @ActivateRequestContext
    fun onMessage(message: Subscribe): String = when (enumValues<ChannelName>().find { it.name == message.stream.uppercase() }) {
        else -> "Wrong channel!"
    }
}
