package dev.mastodon.streaming

/** Sets a field CDI would inject, for values Mockito can't inject such as config properties. */
fun <T : Any> T.withField(name: String, value: Any?): T = apply {
    javaClass.getDeclaredField(name).apply { isAccessible = true }
        .set(this, value)
}
