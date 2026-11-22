package dev.mastodon.streaming.util.primitives

import java.util.Locale.getDefault

/** Query parameters are truthy unless they are empty or one of the known false values. */
fun String?.isTruthy(): Boolean = !isNullOrEmpty() && this.lowercase(getDefault()) !in listOf("0", "f", "false", "off")
