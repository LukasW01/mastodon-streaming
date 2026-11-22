package dev.mastodon.streaming

import dev.mastodon.streaming.dto.AccessToken
import dev.mastodon.streaming.util.session.ConnectionType
import dev.mastodon.streaming.util.session.StreamSession

/** A message a [FakeSession] transmitted to its client. */
data class SentMessage(
    val streamName: List<String>,
    val event: String,
    val payload: String,
)

/** Records what is sent to the client instead of writing to a connection. */
class FakeSession(account: AccessToken) : StreamSession(account, ConnectionType.EVENTSOURCE) {
    val sent: MutableList<SentMessage> = mutableListOf()
    var closed: Boolean = false
        private set

    override fun send(
        streamName: List<String>,
        event: String,
        payload: String
    ) {
        sent += SentMessage(streamName, event, payload)
    }

    override fun close() {
        closed = true
    }
}
