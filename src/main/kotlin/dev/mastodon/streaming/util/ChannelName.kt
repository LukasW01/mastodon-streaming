package dev.mastodon.streaming.util

import dev.mastodon.streaming.dto.StreamParams

/**
 * The streams a client can subscribe to, also used for priming the
 * counters/gauges for the various metrics that are per-channel. Streams with a
 * feed kind need filtering and are subject to the feed access settings.
 */
enum class ChannelName(val value: String, val type: String? = null) {
    SYSTEM("system"),
    USER("user"),
    USER_NOTIFICATION("user:notification"),
    LIST("list"),
    DIRECT("direct"),
    PUBLIC("public", "public"),
    PUBLIC_MEDIA("public:media", "public"),
    PUBLIC_LOCAL("public:local", "public"),
    PUBLIC_LOCAL_MEDIA("public:local:media", "public"),
    PUBLIC_REMOTE("public:remote", "public"),
    PUBLIC_REMOTE_MEDIA("public:remote:media", "public"),
    HASHTAG("hashtag", "hashtag"),
    HASHTAG_LOCAL("hashtag:local", "hashtag"),
    ;

    /** The stream name sent along each websocket message, hashtag and list streams include their parameter. */
    fun streamName(params: StreamParams): List<String> = when {
        this == LIST && !params.list.isNullOrEmpty() -> listOf(value, params.list)
        this in listOf(HASHTAG, HASHTAG_LOCAL) && !params.tag.isNullOrEmpty() -> listOf(value, params.tag)
        else -> listOf(value)
    }

    companion object {
        fun fromValue(value: String?): ChannelName? = entries.find { it.value == value }

        /** @param path the request path below `/api/v1/streaming/` */
        fun fromPath(path: String, onlyMedia: Boolean): ChannelName? = when (path) {
            "user" -> USER
            "user/notification" -> USER_NOTIFICATION
            "public" -> if (onlyMedia) PUBLIC_MEDIA else PUBLIC
            "public/local" -> if (onlyMedia) PUBLIC_LOCAL_MEDIA else PUBLIC_LOCAL
            "public/remote" -> if (onlyMedia) PUBLIC_REMOTE_MEDIA else PUBLIC_REMOTE
            "hashtag" -> HASHTAG
            "hashtag/local" -> HASHTAG_LOCAL
            "direct" -> DIRECT
            "list" -> LIST
            else -> null
        }
    }
}
