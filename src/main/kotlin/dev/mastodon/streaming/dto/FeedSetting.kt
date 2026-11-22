package dev.mastodon.streaming.dto

import io.quarkus.runtime.annotations.RegisterForReflection

@RegisterForReflection
data class FeedSetting(
    val variable: String,
    val value: String?,
)
