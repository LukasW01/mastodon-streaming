package dev.mastodon.streaming.util.session

import dev.mastodon.streaming.dto.AccessToken
import io.smallrye.mutiny.subscription.MultiEmitter
import jakarta.ws.rs.sse.OutboundSseEvent
import jakarta.ws.rs.sse.Sse

class EventSourceSession(
    private val emitter: MultiEmitter<in OutboundSseEvent>,
    private val sse: Sse,
    account: AccessToken,
) : StreamSession(account, ConnectionType.EVENTSOURCE) {
    override fun send(
        streamName: List<String>,
        event: String,
        payload: String
    ) {
        emitter.emit(sse.newEventBuilder().name(event).data(payload)
            .build())
    }

    /** Comments are ignored by clients, they only keep the connection open. */
    fun sendComment(text: String) {
        emitter.emit(sse.newEventBuilder().comment(text).build())
    }

    override fun close() {
        emitter.complete()
    }
}
