package dev.mastodon.streaming.util

object Filter {
    val type: List<String> = listOf("warn", "hide")
    val events: List<String> = listOf("update", "status.update")
}
