package dev.mastodon.streaming.util.primitives

/** Query parameters are truthy unless they are empty or one of the known false values. */
fun String?.isTruthy(): Boolean = !isNullOrEmpty() && this !in listOf("0", "f", "F", "false", "FALSE")
