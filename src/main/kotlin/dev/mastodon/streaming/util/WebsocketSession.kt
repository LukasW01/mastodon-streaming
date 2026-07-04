package dev.mastodon.streaming.util

import dev.mastodon.streaming.dto.AccessToken
import io.quarkus.websockets.next.WebSocketConnection
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import dev.mastodon.streaming.dto.CachedFilter
import dev.mastodon.streaming.dto.RedisMessage

data class WsSubscription(
    val channelName: ChannelName,
    val listener: (RedisMessage) -> Unit,
)

class WsSession(
    val connectionId: String,
    val ws: WebSocketConnection,
    val account: AccessToken,
) {
    val subscriptions = ConcurrentHashMap<String, WsSubscription>()
    val cachedFiltersRef = AtomicReference<Map<Long, CachedFilter>?>(null)
}