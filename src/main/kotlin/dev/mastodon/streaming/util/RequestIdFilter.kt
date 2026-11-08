package dev.mastodon.streaming.util

import io.quarkus.vertx.http.runtime.filters.Filters
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import java.util.UUID

/** Set the X-Request-Id header on every response, including WebSocket upgrades. */
@ApplicationScoped
class RequestIdFilter {
    fun register(@Observes filters: Filters) {
        filters.register({ context ->
            context.response().putHeader("X-Request-Id", UUID.randomUUID().toString())
            context.next()
        }, FILTER_PRIORITY)
    }

    private companion object {
        const val FILTER_PRIORITY = 100
    }
}
