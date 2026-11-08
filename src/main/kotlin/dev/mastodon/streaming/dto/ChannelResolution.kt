package dev.mastodon.streaming.dto

import dev.mastodon.streaming.util.ChannelName

/** A stream resolved to the Redis channels it streams from, and how their messages have to be filtered. */
data class ChannelResolution(
    val channelName: ChannelName,
    val params: StreamParams,
    val channelIds: List<String>,
    val needsFiltering: Boolean,
    val filterLocal: Boolean = false,
    val filterRemote: Boolean = false,
) {
    val streamName: List<String> = channelName.streamName(params)
}
