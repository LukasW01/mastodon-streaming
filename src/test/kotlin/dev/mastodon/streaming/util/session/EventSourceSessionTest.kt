package dev.mastodon.streaming.util.session

import dev.mastodon.streaming.dto.AccessToken

import io.smallrye.mutiny.subscription.MultiEmitter
import jakarta.ws.rs.sse.OutboundSseEvent
import org.jboss.resteasy.reactive.server.jaxrs.SseImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify

@ExtendWith(MockitoExtension::class)
class EventSourceSessionTest {
    private val account = AccessToken(tokenId = 10, resourceOwnerId = 20, accountId = 30, scopes = "read", permissions = 0)
    private val session by lazy { EventSourceSession(emitter, SseImpl.INSTANCE, account) }

    @Mock
    private lateinit var emitter: MultiEmitter<OutboundSseEvent>

    @Test
    fun `messages are emitted as named events`() {
        session.send(listOf("public"), "update", """{"id":"1"}""")

        val event = argumentCaptor<OutboundSseEvent>().apply { verify(emitter).emit(capture()) }.firstValue
        assertEquals("update", event.name)
        assertEquals("""{"id":"1"}""", event.data)
    }

    @Test
    fun `comments are emitted without event name`() {
        session.sendComment("thump")

        val event = argumentCaptor<OutboundSseEvent>().apply { verify(emitter).emit(capture()) }.firstValue
        assertEquals("thump", event.comment)
        assertNull(event.name)
    }

    @Test
    fun `closing the session completes the stream`() {
        session.close()

        verify(emitter).complete()
    }

    @Test
    fun `eventsource sessions are counted as eventsource connections`() {
        assertEquals(ConnectionType.EVENTSOURCE, session.type)
    }
}
