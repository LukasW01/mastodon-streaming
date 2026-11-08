package dev.mastodon.streaming.service

import dev.mastodon.streaming.dto.ChannelResolution
import dev.mastodon.streaming.dto.RedisMessage
import dev.mastodon.streaming.util.ChannelName
import dev.mastodon.streaming.util.session.StreamSession
import dev.mastodon.streaming.util.session.Subscription
import dev.mastodon.streaming.util.session.SubscriptionListener

import com.fasterxml.jackson.databind.JsonNode
import dev.mastodon.streaming.prometheus.Metrics
import io.quarkus.logging.Log
import io.quarkus.virtual.threads.VirtualThreads
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

import java.util.concurrent.ExecutorService

private const val CHANNEL_IDS_SEPARATOR = ";"
private val filteredEvents = listOf("update", "status.update")

/** Connects the sessions of clients with the Redis channels they stream from. */
@ApplicationScoped
class StreamService {
    @Inject
    private lateinit var redisService: RedisService

    @Inject
    private lateinit var statusFilterService: StatusFilterService

    @Inject
    private lateinit var metrics: Metrics

    @Inject
    @VirtualThreads
    private lateinit var executor: ExecutorService

    fun open(session: StreamSession) {
        // In case the handler throws, which would terminate the connection,
        // increment the connected clients metric straight away when it establishes
        // the connection, without waiting:
        metrics.connectedClients(session.type).incrementAndGet()

        subscribeToSystemChannel(session)
    }

    fun close(session: StreamSession) {
        session.subscriptions.keys.forEach { channelIds ->
            removeSubscription(session, channelIds.split(CHANNEL_IDS_SEPARATOR))
        }

        // Decrement the metrics for connected clients:
        metrics.connectedClients(session.type).decrementAndGet()
    }

    fun subscribeToChannel(session: StreamSession, resolution: ChannelResolution) {
        val key = resolution.channelIds.joinToString(CHANNEL_IDS_SEPARATOR)

        if (key in session.subscriptions) {
            return
        }

        val stopHeartbeat = redisService.subscriptionHeartbeat(resolution.channelIds)
        val listener = streamFrom(session, resolution)

        metrics.connectedChannels(session.type, resolution.channelName).incrementAndGet()

        session.subscriptions[key] = Subscription(resolution.channelName, listener, stopHeartbeat)
    }

    fun removeSubscription(session: StreamSession, channelIds: List<String>) {
        Log.infof("Ending stream %s for account %s", channelIds, session.account.accountId)

        val subscription = session.subscriptions.remove(channelIds.joinToString(CHANNEL_IDS_SEPARATOR)) ?: return

        channelIds.forEach { channelId -> redisService.unsubscribe(channelId, subscription.listener) }

        metrics.connectedChannels(session.type, subscription.channelName).decrementAndGet()
        subscription.stopHeartbeat()
    }

    private fun subscribeToSystemChannel(session: StreamSession) {
        val channelIds = listOf("timeline:access_token:${session.account.tokenId}", "timeline:system:${session.account.accountId}")
        val listener = systemMessageListener(session)

        channelIds.forEach { channelId ->
            redisService.subscribe(channelId, listener)
            session.subscriptions[channelId] = Subscription(ChannelName.SYSTEM, listener) { }
        }

        metrics.connectedChannels(session.type, ChannelName.SYSTEM).addAndGet(channelIds.size.toLong())
    }

    private fun systemMessageListener(session: StreamSession): SubscriptionListener = { message ->
        val accountId = session.account.accountId

        Log.debugf("System message for %s: %s", accountId, message.event)

        when (message.event) {
            "kill" -> {
                Log.debugf("Closing connection for %s due to expired access token", accountId)
                session.close()
            }
            "filters_changed" -> {
                Log.debugf("Invalidating filters cache for %s", accountId)
                session.cachedFiltersRef.set(null)
            }
        }
    }

    private fun streamFrom(session: StreamSession, resolution: ChannelResolution): SubscriptionListener {
        Log.infof("Starting stream %s for account %s", resolution.channelIds, session.account.accountId)

        val listener = StreamListener(session, resolution)

        resolution.channelIds.forEach { id -> redisService.subscribe(id, listener) }

        return listener
    }

    /**
     * The listener used to process each message off the redis subscription,
     * message here is an object with an `event` and `payload` property.
     */
    private inner class StreamListener(
        private val session: StreamSession,
        private val resolution: ChannelResolution,
    ) : SubscriptionListener {
        override fun invoke(message: RedisMessage) {
            val event = message.event
            val payload = message.payload

            if (event.isNullOrEmpty() || payload == null || payload.isNull) {
                return
            }

            // Streaming only needs to apply filtering to some channels and only to
            // some events. This is because majority of the filtering happens on the
            // Ruby on Rails side when producing the event for streaming.
            //
            // The only events that require filtering from the streaming server are
            // `update` and `status.update`, all other events are transmitted to the
            // client as soon as they're received (pass-through).
            //
            // The channels that need filtering are determined in
            // `ChannelService.channelNameToIds`:
            if (!resolution.needsFiltering || event !in filteredEvents) {
                transmit(event, payload)
                return
            }

            // The filters need the database, which must not block the Redis connection:
            executor.execute { filterAndTransmit(event, payload) }
        }

        private fun filterAndTransmit(event: String, payload: JsonNode) {
            try {
                statusFilterService.filter(session, resolution, payload)?.let { transmit(event, it) }
            } catch (err: RuntimeException) {
                Log.error("Error filtering message", err)
            }
        }

        private fun transmit(event: String, payload: JsonNode) {
            // TODO: Replace "string"-based delete payloads with object payloads:
            val encodedPayload = if (payload.isTextual) payload.textValue() else payload.toString()

            metrics.messagesSent(session.type).increment()

            Log.debugf("Transmitting %s to %s", event, session.account.accountId)

            session.send(resolution.streamName, event, encodedPayload)
        }
    }
}
