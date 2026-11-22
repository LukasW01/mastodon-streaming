package dev.mastodon.streaming.service

import dev.mastodon.streaming.dto.RedisMessage
import dev.mastodon.streaming.withField

import io.quarkus.redis.datasource.ReactiveRedisDataSource
import io.quarkus.redis.datasource.pubsub.ReactivePubSubCommands
import io.quarkus.redis.datasource.pubsub.ReactivePubSubCommands.ReactiveRedisSubscriber
import io.quarkus.redis.datasource.value.ReactiveValueCommands
import io.quarkus.runtime.StartupEvent
import io.smallrye.mutiny.Uni
import io.vertx.core.Handler
import io.vertx.core.Vertx
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

import java.util.Optional
import java.util.function.BiConsumer
import java.util.function.Consumer

@ExtendWith(MockitoExtension::class)
class RedisServiceTest {
    private val message = RedisMessage("update")

    @Mock
    private lateinit var redisDataSource: ReactiveRedisDataSource

    @Mock
    private lateinit var vertx: Vertx

    @Mock
    private lateinit var pubSub: ReactivePubSubCommands<RedisMessage>

    @Mock
    private lateinit var values: ReactiveValueCommands<String, String>

    @InjectMocks
    private lateinit var redisService: RedisService

    /** @return the callback that receives the messages of the pattern subscription */
    private fun start(namespace: String? = null): BiConsumer<String, RedisMessage> {
        redisService.withField("namespace", Optional.ofNullable(namespace))
        whenever(redisDataSource.pubsub(RedisMessage::class.java)).thenReturn(pubSub)
        whenever(pubSub.subscribeToPattern(any(), any<BiConsumer<String, RedisMessage>>(), any(), any<Consumer<Throwable>>()))
            .thenReturn(Uni.createFrom().item(mock<ReactiveRedisSubscriber>()))

        redisService.start(StartupEvent())

        return argumentCaptor<BiConsumer<String, RedisMessage>>().apply {
            verify(pubSub).subscribeToPattern(eq(listOfNotNull(namespace, "timeline:*").joinToString(":")), capture(), any(), any<Consumer<Throwable>>())
        }.firstValue
    }

    private fun heartbeat(namespace: String? = null, result: Uni<Void> = Uni.createFrom().voidItem()) {
        redisService.withField("namespace", Optional.ofNullable(namespace))
        whenever(redisDataSource.value(String::class.java)).thenReturn(values)
        whenever(values.setex(any(), any(), any())).thenReturn(result)
    }

    @Test
    fun `messages are delivered to the listeners of their channel`() {
        val onMessage = start()
        val received: MutableList<RedisMessage> = mutableListOf()
        redisService.subscribe("timeline:public") { received += it }

        onMessage.accept("timeline:public", message)
        onMessage.accept("timeline:public:local", RedisMessage("delete"))

        assertEquals(listOf(message), received)
    }

    @Test
    fun `the namespace is added to the pattern and removed from channels`() {
        val onMessage = start("mastodon")
        val received: MutableList<RedisMessage> = mutableListOf()
        redisService.subscribe("timeline:public") { received += it }

        onMessage.accept("mastodon:timeline:public", message)

        assertEquals(listOf(message), received)
    }

    @Test
    fun `a failing listener does not prevent delivery to the others`() {
        val onMessage = start()
        redisService.subscribe("timeline:public") { throw IllegalStateException("Connection closed") }
        val received: MutableList<RedisMessage> = mutableListOf()
        redisService.subscribe("timeline:public") { received += it }

        onMessage.accept("timeline:public", message)

        assertEquals(listOf(message), received)
    }

    @Test
    fun `listeners are only registered once per channel`() {
        val onMessage = start()
        val received: MutableList<RedisMessage> = mutableListOf()
        val listener: (RedisMessage) -> Unit = { received += it }
        redisService.subscribe("timeline:public", listener)
        redisService.subscribe("timeline:public", listener)

        onMessage.accept("timeline:public", message)

        assertEquals(listOf(message), received)
    }

    @Test
    fun `unsubscribed listeners no longer receive messages`() {
        val onMessage = start()
        val removed: MutableList<RedisMessage> = mutableListOf()
        val kept: MutableList<RedisMessage> = mutableListOf()
        val removedListener: (RedisMessage) -> Unit = { removed += it }
        val keptListener: (RedisMessage) -> Unit = { kept += it }
        redisService.subscribe("timeline:public", removedListener)
        redisService.subscribe("timeline:public", keptListener)

        redisService.unsubscribe("timeline:public", removedListener)
        onMessage.accept("timeline:public", message)
        redisService.unsubscribe("timeline:public", keptListener)
        onMessage.accept("timeline:public", message)

        assertTrue(removed.isEmpty())
        assertEquals(listOf(message), kept)
    }

    @Test
    fun `unsubscribing from unknown channels is ignored`() {
        redisService.unsubscribe("timeline:public") { }
    }

    @Test
    fun `the heartbeat marks channels as subscribed until it is stopped`() {
        heartbeat()
        whenever(vertx.setPeriodic(eq(30_000L), any())).thenReturn(7L)

        val stop = redisService.subscriptionHeartbeat(listOf("timeline:public", "timeline:hashtag:kotlin"))

        verify(values).setex("subscribed:timeline:public", 90, "1")
        verify(values).setex("subscribed:timeline:hashtag:kotlin", 90, "1")

        argumentCaptor<Handler<Long>>().apply { verify(vertx).setPeriodic(eq(30_000L), capture()) }.firstValue.handle(7L)
        verify(values, times(2)).setex("subscribed:timeline:public", 90, "1")

        stop()
        verify(vertx).cancelTimer(7L)
    }

    @Test
    fun `the heartbeat uses the namespace`() {
        heartbeat("mastodon")

        redisService.subscriptionHeartbeat(listOf("timeline:public"))

        verify(values).setex("mastodon:subscribed:timeline:public", 90, "1")
    }

    @Test
    fun `heartbeat failures are not thrown`() {
        heartbeat(result = Uni.createFrom().failure(IllegalStateException("Redis unavailable")))

        redisService.subscriptionHeartbeat(listOf("timeline:public"))

        verify(vertx).setPeriodic(any(), anyOrNull())
    }
}
