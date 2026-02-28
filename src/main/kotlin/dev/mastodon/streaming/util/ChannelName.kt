package dev.mastodon.streaming.util

enum class ChannelName(val value: String) {
    SYSTEM("system"),
    USER("user"),
    USER_NOTIFICATION("user:notification"),
    LIST("list"),
    DIRECT("direct"),
    PUBLIC("public"),
    PUBLIC_MEDIA("public:media"),
    PUBLIC_LOCAL("public:local"),
    PUBLIC_LOCAL_MEDIA("public:local:media"),
    PUBLIC_REMOTE("public:remote"),
    PUBLIC_REMOTE_MEDIA("public:remote:media"),
    HASHTAG("hashtag"),
    HASHTAG_LOCAL("hashtag:local"),
    ;
}
