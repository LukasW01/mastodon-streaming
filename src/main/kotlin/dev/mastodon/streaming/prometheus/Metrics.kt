package dev.mastodon.streaming.prometheus

import dev.mastodon.streaming.util.ChannelName
import dev.mastodon.streaming.util.session.ConnectionType
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.quarkus.runtime.Startup
import jakarta.annotation.PostConstruct
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

private typealias ChannelMetric = Pair<ConnectionType, ChannelName>

@Startup
@ApplicationScoped
class Metrics {
    private val connectedClients: MutableMap<ConnectionType, AtomicLong> = ConcurrentHashMap()
    private val connectedChannels: MutableMap<ChannelMetric, AtomicLong> = ConcurrentHashMap()
    private val messagesSent: MutableMap<ConnectionType, Counter> = ConcurrentHashMap()

    @Inject
    private lateinit var registry: MeterRegistry

    @PostConstruct
    fun setup() {
        // Prime the gauges and counters so we don't loose metrics between restarts.
        // For each channel, initialize the gauges at zero; There's only a finite set of channels available
        ConnectionType.entries.forEach { type ->
            connectedClients(type)
            messagesSent(type)
            ChannelName.entries.forEach { channel -> connectedChannels(type, channel) }
        }
    }

    fun connectedClients(type: ConnectionType): AtomicLong = connectedClients.computeIfAbsent(type) {
        AtomicLong().also { value ->
            Gauge.builder("connected_clients", value, AtomicLong::toDouble)
                .description("The number of clients connected to the streaming server")
                .tag("type", type.value)
                .register(registry)
        }
    }

    fun connectedChannels(type: ConnectionType, channel: ChannelName): AtomicLong = connectedChannels.computeIfAbsent(type to channel) {
        AtomicLong().also { value ->
            Gauge.builder("connected_channels", value, AtomicLong::toDouble)
                .description("The number of channels the streaming server is streaming to")
                .tag("type", type.value)
                .tag("channel", channel.value)
                .register(registry)
        }
    }

    fun messagesSent(type: ConnectionType): Counter = messagesSent.computeIfAbsent(type) {
        Counter.builder("messages_sent")
            .description("The total number of messages the streaming server sent to clients per connection type")
            .tag("type", type.value)
            .register(registry)
    }
}
