package dev.mastodon.streaming.util.session

import dev.mastodon.streaming.dto.AccessToken
import dev.mastodon.streaming.dto.StreamMessage
import dev.mastodon.streaming.util.primitives.logError

import io.quarkus.logging.Log
import io.quarkus.websockets.next.WebSocketConnection

/** Messages are encoded as JSON by the text message codec of websockets next. */
class WsSession(
    private val ws: WebSocketConnection,
    account: AccessToken,
) : StreamSession(account, ConnectionType.WEBSOCKET) {
    override fun send(
        streamName: List<String>,
        event: String,
        payload: String
    ) {
        sendCustom(StreamMessage(streamName, event, payload))
    }

    fun sendCustom(message: Any) {
        if (!ws.isOpen) {
            Log.error("Tried writing to closed socket (ID: ${ws.id()})")
            return
        }

        ws.sendText(message).logError("Failed to send to websocket")
    }

    override fun close() {
        ws.close().logError("Failed to close websocket")
    }
}
