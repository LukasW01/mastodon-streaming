package dev.mastodon.streaming.util.session

fun StreamSession.channelIds(): List<String> = listOf("timeline:access_token:${this.account.tokenId}", "timeline:system:${this.account.accountId}")
