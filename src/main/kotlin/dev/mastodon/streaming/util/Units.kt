package dev.mastodon.streaming.util

object Units {
    const val HEARTBEAT_EXPIRY_FACTOR = 3
    const val HEARTBEAT_INTERVAL_SECONDS = 30L
    const val RECONNECT_MAX_BACKOFF_SECONDS = 30L
    const val RECONNECT_DELAY_SECONDS = 1L
}
