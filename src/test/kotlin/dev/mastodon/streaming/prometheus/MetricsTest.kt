package dev.mastodon.streaming.prometheus

import dev.mastodon.streaming.util.ChannelName
import dev.mastodon.streaming.util.session.ConnectionType

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class MetricsTest {
    @Spy
    private val registry: MeterRegistry = SimpleMeterRegistry()

    @InjectMocks
    private lateinit var metrics: Metrics

    private fun gauge(name: String, vararg tags: String): Double = registry.get(name)
        .tags(*tags)
        .gauge()
        .value()

    private fun counter(name: String, vararg tags: String): Double = registry.get(name)
        .tags(*tags)
        .counter()
        .count()

    @Test
    fun `all metrics are registered at zero on startup`() {
        metrics.setup()

        ConnectionType.entries.forEach { type ->
            assertEquals(0.0, gauge("connected_clients", "type", type.value))
            assertEquals(0.0, counter("messages_sent", "type", type.value))
            ChannelName.entries.forEach { channel ->
                assertEquals(0.0, gauge("connected_channels", "type", type.value, "channel", channel.value))
            }
        }
        assertEquals(ConnectionType.entries.size * (ChannelName.entries.size + 2), registry.meters.size)
    }

    @Test
    fun `gauges report the current values`() {
        metrics.connectedClients(ConnectionType.WEBSOCKET).addAndGet(3)
        metrics.connectedChannels(ConnectionType.EVENTSOURCE, ChannelName.HASHTAG).incrementAndGet()
        metrics.messagesSent(ConnectionType.WEBSOCKET).increment(2.0)

        assertEquals(3.0, gauge("connected_clients", "type", "websocket"))
        assertEquals(1.0, gauge("connected_channels", "type", "eventsource", "channel", "hashtag"))
        assertEquals(2.0, counter("messages_sent", "type", "websocket"))
    }

    @Test
    fun `each metric is only created once`() {
        assertSame(metrics.connectedClients(ConnectionType.WEBSOCKET), metrics.connectedClients(ConnectionType.WEBSOCKET))
        assertSame(metrics.connectedChannels(ConnectionType.WEBSOCKET, ChannelName.USER), metrics.connectedChannels(ConnectionType.WEBSOCKET, ChannelName.USER))
        assertSame(metrics.messagesSent(ConnectionType.WEBSOCKET), metrics.messagesSent(ConnectionType.WEBSOCKET))
    }
}
