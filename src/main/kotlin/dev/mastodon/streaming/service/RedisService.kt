package dev.mastodon.streaming.service

import dev.mastodon.streaming.dto.RedisMessage
import dev.mastodon.streaming.util.session.SubscriptionListener

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.logging.Log
import io.quarkus.redis.datasource.ReactiveRedisDataSource
import io.quarkus.runtime.ShutdownEvent
import io.vertx.core.Vertx
import io.vertx.mutiny.redis.client.Command
import io.vertx.mutiny.redis.client.Redis
import io.vertx.mutiny.redis.client.RedisConnection
import io.vertx.mutiny.redis.client.Request
import io.vertx.mutiny.redis.client.Response
import jakarta.annotation.PostConstruct
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.enterprise.event.Reception
import jakarta.inject.Inject
import org.eclipse.microprofile.config.inject.ConfigProperty

import java.time.Duration
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

private const val MESSAGE_TYPE = "message"
private const val MESSAGE_CHANNEL_INDEX = 1
private const val MESSAGE_CONTENT_INDEX = 2
private const val HEARTBEAT_EXPIRY_FACTOR = 3
private const val HEARTBEAT_INTERVAL_MINUTES = 6L
private const val RECONNECT_MAX_BACKOFF_SECONDS = 30L
private val heartbeatInterval: Duration = Duration.ofMinutes(HEARTBEAT_INTERVAL_MINUTES)
private val reconnectMinBackoff: Duration = Duration.ofSeconds(1)
private val reconnectMaxBackoff: Duration = Duration.ofSeconds(RECONNECT_MAX_BACKOFF_SECONDS)

private typealias Listeners = MutableSet<SubscriptionListener>

/**
 * Multiplexes all clients over a single Redis subscriber connection, as
 * `ReactivePubSubCommands` would open one connection per subscribed channel.
 * Each channel is subscribed to once, the messages are handed to its listeners.
 * The connection is opened by the first subscription.
 */
@ApplicationScoped
class RedisService {
    private val subs: ConcurrentHashMap<String, Listeners> = ConcurrentHashMap()

    @Volatile
    private var connection: RedisConnection? = null

    @Volatile
    private var stopped = false

    @Inject
    private lateinit var redis: Redis

    @Inject
    private lateinit var redisDataSource: ReactiveRedisDataSource

    @Inject
    private lateinit var vertx: Vertx
    
    @Inject
    private lateinit var objectMapper: ObjectMapper

    @ConfigProperty(name = "mastodon.redis.namespace")
    private lateinit var namespace: Optional<String>

    @PostConstruct
    fun connect() {
        redis.connect()
            .onFailure()
            .invoke { err -> Log.error("Redis Client Error!", err) }
            .onFailure()
            .retry()
            .withBackOff(reconnectMinBackoff, reconnectMaxBackoff)
            .indefinitely()
            .subscribe()
            .with(::onConnected) { err -> Log.error("Redis Client Error!", err) }
    }

    private fun onConnected(redisConnection: RedisConnection) {
        redisConnection.handler(::onResponse)
        redisConnection.exceptionHandler { err -> Log.error("Redis Client Error!", err) }
        redisConnection.endHandler(::onDisconnected)
        connection = redisConnection

        // Subscriptions don't survive the connection, restore them after a reconnect
        subs.keys.forEach { channel -> send(Command.SUBSCRIBE, channel) }
    }

    private fun onDisconnected() {
        connection = null
        if (!stopped) {
            connect()
        }
    }

    /** The Redis client closes its connections during shutdown, which must not trigger a reconnect. */
    fun stop(@Observes(notifyObserver = Reception.IF_EXISTS) event: ShutdownEvent) {
        stopped = true
    }

    /**
     * Adds a namespace to Redis keys or channel names
     * Note: we don't use auto-prefixing of keys since this doesn't apply to
     * subscribe/unsubscribe which have "channel" instead of "key" arguments
     */
    private fun redisNamespaced(keyOrChannel: String): String? =
        namespace.map { "$it:$keyOrChannel" }.orElse(keyOrChannel)

    /** Removes the redis namespace from a channel name */
    private fun redisUnnamespaced(channel: String): String? =
        // Note: this removes the configured namespace and the colon that is used
        // to separate it:
        namespace.map { channel.removePrefix("$it:") }.orElse(channel)

    private fun onResponse(response: Response) {
        // Subscribe and unsubscribe confirmations also arrive here
        if (response.size() > MESSAGE_CONTENT_INDEX && response.get(0).toString() == MESSAGE_TYPE) {
            onRedisMessage(response.get(MESSAGE_CHANNEL_INDEX).toString(), response.get(MESSAGE_CONTENT_INDEX).toString())
        }
    }

    fun onRedisMessage(channel: String, message: String) {
        Log.debugf("New message on channel %s", channel)

        val key = redisUnnamespaced(channel)
        val callbacks = subs[key] ?: return

        val json = parseJson(message) ?: return

        callbacks.forEach { callback ->
            runCatching { callback(json) }.onFailure { err -> Log.error("Error handling message on channel $key", err) }
        }
    }

    /** Attempts to safely parse a string as JSON, used when receiving a message from redis. */
    private fun parseJson(message: String): RedisMessage? = try {
        objectMapper.readValue(message, RedisMessage::class.java)
    } catch (err: JsonProcessingException) {
        Log.error("Error parsing message from redis", err)
        null
    }

    fun subscribe(channel: String, callback: SubscriptionListener) {
        Log.debugf("Adding listener for %s", channel)

        subs.compute(channel) { _, listeners ->
            (listeners ?: ConcurrentHashMap.newKeySet()).apply {
                if (isEmpty()) {
                    Log.debugf("Subscribe %s", channel)
                    send(Command.SUBSCRIBE, channel)
                }
                add(callback)
            }
        }
    }

    fun unsubscribe(channel: String, callback: SubscriptionListener) {
        Log.debugf("Removing listener for %s", channel)

        subs.computeIfPresent(channel) { _, listeners ->
            listeners.remove(callback)
            listeners.takeUnless { it.isEmpty() } ?: run {
                Log.debugf("Unsubscribe %s", channel)
                send(Command.UNSUBSCRIBE, channel)
                null
            }
        }
    }

    /**
     * While the connection is down there is nothing to do, [onConnected]
     * subscribes to all channels again.
     */
    private fun send(command: Command, channel: String) {
        connection?.send(Request.cmd(command).arg(redisNamespaced(channel)))
            ?.subscribe()?.with({ }, { err -> Log.error("Error sending $command to $channel", err) })
    }

    /**
     * Tells Rails which timelines have an active stream.
     *
     * @return a function that stops the heartbeat
     */
    fun subscriptionHeartbeat(channels: List<String>): () -> Unit {
        val tellSubscribed = {
            channels.forEach { channel ->
                redisDataSource.value(String::class.java)
                    .setex(redisNamespaced("subscribed:$channel"), heartbeatInterval.seconds * HEARTBEAT_EXPIRY_FACTOR, "1")
                    .subscribe().with({ }, { err -> Log.error("Error sending heartbeat for $channel", err) })
            }
        }

        tellSubscribed()

        val heartbeat = vertx.setPeriodic(heartbeatInterval.toMillis()) { tellSubscribed() }

        return { vertx.cancelTimer(heartbeat) }
    }
}
