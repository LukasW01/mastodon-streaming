package dev.mastodon.streaming.util.session

import dev.mastodon.streaming.dto.AccessToken
import dev.mastodon.streaming.dto.StreamMessage

import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.logging.Log
import io.quarkus.websockets.next.WebSocketConnection

class WsSession(
    private val ws: WebSocketConnection,
    account: AccessToken,
) : StreamSession(account, ConnectionType.WEBSOCKET) {
    private lateinit var objectMapper: ObjectMapper

    override fun send(
        streamName: List<String>,
        event: String,
        payload: String
    ) {
        if (!ws.isOpen) {
            Log.error("Tried writing to closed socket (ID: ${ws.id()})")
            return
        }

        ws.sendText(
            objectMapper.writeValueAsString(StreamMessage(streamName, event, payload))
        ).log("Failed to send to websocket")
    }

    fun sendCustom(message: Any) {
        if (!ws.isOpen) {
            Log.error("Tried writing to closed socket (ID: ${ws.id()})")
            return
        }

        ws.sendText(
            objectMapper.writeValueAsString(message)
        ).log("Failed to send to websocket")
    }

    override fun close() {
        ws.close().log("Failed to close websocket")
    }
}
