package dev.mastodon.streaming.util

import dev.mastodon.streaming.dto.AccessToken
import dev.mastodon.streaming.dto.CachedFilter
import dev.mastodon.streaming.dto.RedisMessage

import io.quarkus.websockets.next.WebSocketConnection

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

typealias CachedFilterMap = Map<Long, CachedFilter>?
typealias CachedFilterMapRef = AtomicReference<CachedFilterMap>
typealias WebsocketSubscription = MutableMap<String, WsSubscription>

data class WsSubscription(
    val channelName: ChannelName,
    val listener: (RedisMessage) -> Unit,
)

data class WsSession(
    val connectionId: String,
    val ws: WebSocketConnection,
    val account: AccessToken,
) {
    val subscriptions: WebsocketSubscription = ConcurrentHashMap()
    val cachedFiltersRef: CachedFilterMapRef = AtomicReference(null)
}
