package dev.mastodon.streaming.util.session

import dev.mastodon.streaming.dto.AccessToken
import dev.mastodon.streaming.dto.CachedFilter
import dev.mastodon.streaming.dto.RedisMessage
import dev.mastodon.streaming.util.ChannelName
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

typealias CachedFilterMap = Map<Long, CachedFilter>?
typealias CachedFilterMapRef = AtomicReference<CachedFilterMap>
typealias SubscriptionListener = (RedisMessage) -> Unit

enum class ConnectionType(val value: String) {
    WEBSOCKET("websocket"),
    EVENTSOURCE("eventsource"),
    ;
}

data class Subscription(
    val channelName: ChannelName,
    val listener: SubscriptionListener,
    val stopHeartbeat: () -> Unit,
)

/**
 * A client streaming from this server, either over a websocket or over
 * eventsource. Everything else in the streaming pipeline is independent of
 * the transport.
 */
abstract class StreamSession(val account: AccessToken, val type: ConnectionType) {
    /** The active subscriptions, by their channel ids joined with a `;`. */
    val subscriptions: MutableMap<String, Subscription> = ConcurrentHashMap()
    val cachedFiltersRef: CachedFilterMapRef = AtomicReference(null)

    abstract fun send(
        streamName: List<String>,
        event: String,
        payload: String
    )

    abstract fun close()
}
