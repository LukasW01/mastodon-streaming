package dev.mastodon.streaming.util.primitives

import io.quarkus.logging.Log
import io.smallrye.mutiny.Uni

fun Uni<Void>.logError(message: String) {
    subscribe().with({ }, { err -> Log.error(message, err) })
}
