package dev.mastodon.streaming.service

import dev.mastodon.streaming.dto.RedisMessage
import dev.mastodon.streaming.util.Units
import dev.mastodon.streaming.util.session.SubscriptionListener

import io.quarkus.logging.Log
import io.quarkus.redis.datasource.ReactiveRedisDataSource
import io.quarkus.runtime.StartupEvent
import io.vertx.core.Vertx
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import org.eclipse.microprofile.config.inject.ConfigProperty

import java.time.Duration
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

private typealias Listeners = MutableSet<SubscriptionListener>

/**
 * Multiplexes all clients over a single Redis subscriber connection. Every
 * `subscribe` of `ReactivePubSubCommands` takes its own connection from the
 * pool and can't be extended by further channels, so a single pattern
 * subscription covering all `timeline:*` channels is opened on startup, and
 * each message is handed to the listeners of its channel.
 */
@Suppress("TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
@ApplicationScoped
class RedisService {
    private val subs: ConcurrentHashMap<String, Listeners> = ConcurrentHashMap()

    @Inject
    private lateinit var redisDataSource: ReactiveRedisDataSource

    @Inject
    private lateinit var vertx: Vertx

    @ConfigProperty(name = "mastodon.redis.namespace")
    private lateinit var namespace: Optional<String>

    fun start(@Observes event: StartupEvent) {
        connect()
    }

    private fun connect() {
        redisDataSource.pubsub(RedisMessage::class.java)
            .subscribeToPattern(redisNamespaced("timeline:*"), ::onRedisMessage, {}) { err -> Log.error("Redis Client Error!", err) }
            .onFailure()
            .invoke { err -> Log.error("Redis Client Error!", err) }
            .onFailure()
            .retry()
            .withBackOff(Duration.ofSeconds(Units.RECONNECT_DELAY_SECONDS), Duration.ofSeconds(Units.RECONNECT_MAX_BACKOFF_SECONDS))
            .indefinitely()
            .subscribe()
            .with({ Log.info("Subscribed to Redis") }) { err -> Log.error("Redis Client Error!", err) }
    }

    /**
     * The pattern subscription also receives the messages of channels nobody
     * streams from, so a message is only parsed once its channel has listeners.
     */
    private fun onRedisMessage(namespacedChannel: String, message: RedisMessage) {
        redisUnnamespaced(namespacedChannel).let { channel ->
            Log.debugf("New message on channel %s", channel)

            subs[channel]?.forEach { listener ->
                runCatching { listener(message) }.onFailure { err -> Log.error("Error handling message on channel $channel", err) }
            }
        }
    }

    fun subscribe(channel: String, listener: SubscriptionListener) {
        Log.debugf("Adding listener for %s", channel)

        subs.compute(channel) { _, listeners ->
            (listeners ?: ConcurrentHashMap.newKeySet()).apply { add(listener) }
        }
    }

    fun unsubscribe(channel: String, listener: SubscriptionListener) {
        Log.debugf("Removing listener for %s", channel)

        subs.computeIfPresent(channel) { _, listeners ->
            listeners.apply { remove(listener) }.takeUnless { it.isEmpty() }
        }
    }

    /**
     * Adds a namespace to Redis keys or channel names
     * Note: we don't use auto-prefixing of keys since this doesn't apply to
     * subscribe/unsubscribe which have "channel" instead of "key" arguments
     */
    private fun redisNamespaced(keyOrChannel: String): String =
        namespace.map { "$it:$keyOrChannel" }.orElse(keyOrChannel)

    /**
     * Removes the redis namespace from a channel name
     * Note: this removes the configured namespace and the colon that is used
     * to separate it
     */
    private fun redisUnnamespaced(channel: String): String =
        namespace.map { channel.removePrefix("$it:") }.orElse(channel)

    /**
     * Tells Rails which timelines have an active stream.
     *
     * @return a function that stops the heartbeat
     */
    fun subscriptionHeartbeat(channels: List<String>): () -> Unit {
        sendHeartbeat(channels)

        vertx.setPeriodic(Duration.ofSeconds(Units.HEARTBEAT_INTERVAL_SECONDS).toMillis()) { sendHeartbeat(channels) }.let { heartbeat ->
            return { vertx.cancelTimer(heartbeat) }
        }
    }

    private fun sendHeartbeat(channels: List<String>) = channels.forEach { channel ->
        redisDataSource.value(String::class.java)
            .setex(redisNamespaced("subscribed:$channel"), Duration.ofSeconds(Units.HEARTBEAT_INTERVAL_SECONDS).seconds * Units.HEARTBEAT_EXPIRY_FACTOR, "1")
            .subscribe()
            .with({ }) { err -> Log.error("Error sending heartbeat for $channel", err) }
    }
}
