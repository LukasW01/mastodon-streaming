package dev.mastodon.streaming.service

import dev.mastodon.streaming.db.list.ListService
import dev.mastodon.streaming.db.setting.SettingService
import dev.mastodon.streaming.dto.AccessToken
import dev.mastodon.streaming.dto.ChannelResolution
import dev.mastodon.streaming.dto.StreamParams
import dev.mastodon.streaming.util.ChannelName
import dev.mastodon.streaming.util.Filter
import dev.mastodon.streaming.util.Permissions
import dev.mastodon.streaming.util.mapper.WebApplicationMapperException
import dev.mastodon.streaming.util.primitives.normalizeHashtag

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.context.control.ActivateRequestContext
import jakarta.inject.Inject
import jakarta.ws.rs.core.Response

/** Resolves the stream a client asked for to the Redis channels it streams from. */
@ApplicationScoped
class ChannelService {
    @Inject
    private lateinit var settingService: SettingService

    @Inject
    private lateinit var listService: ListService

    @ActivateRequestContext
    fun channelNameToIds(
        account: AccessToken,
        name: ChannelName?,
        params: StreamParams
    ): ChannelResolution {
        val channelName = name?.takeUnless { it == ChannelName.SYSTEM }
            ?: throw WebApplicationMapperException("Unknown stream type", Response.Status.BAD_REQUEST.statusCode)
        val channelIds = channelIds(account, channelName, params)

        if (channelName.type.equals(null)) {
            return ChannelResolution(channelName, params, channelIds, needsFiltering = false)
        }
        
        if (account.permissions and Permissions.VIEW_FEEDS != 0L) {
            return ChannelResolution(channelName, params, channelIds, needsFiltering = true)
        }

        settingService.findByKind(channelName.type!!)
            .filter { it.value == Filter.disabled }
            .map { it.variable }
            .let { disabled ->
                return ChannelResolution(channelName, params, channelIds, needsFiltering = true).copy(
                    filterLocal = disabled.any { it.startsWith("local_") },
                    filterRemote = disabled.any { it.startsWith("remote_") },
                )
            }
    }
    
    private fun channelIds(
        account: AccessToken,
        name: ChannelName,
        params: StreamParams
    ): List<String> = when (name) {
        ChannelName.USER -> channelsUserStream(account)
        ChannelName.USER_NOTIFICATION -> listOf("timeline:${account.accountId}:notifications")
        ChannelName.DIRECT -> listOf("timeline:direct:${account.accountId}")
        ChannelName.HASHTAG -> listOf("timeline:hashtag:${hashtag(params)}")
        ChannelName.HASHTAG_LOCAL -> listOf("timeline:hashtag:${hashtag(params)}:local")
        ChannelName.LIST -> listOf("timeline:list:${authorizedList(account, params)}")
        else -> listOf("timeline:${name.value}")
    }

    private fun channelsUserStream(account: AccessToken): List<String> = buildList {
        add("timeline:${account.accountId}")

        if (account.isInScope(listOf("read", "read:notifications"))) {
            add("timeline:${account.accountId}:notifications")
        }
    }

    private fun hashtag(params: StreamParams): String {
        if (params.tag.isNullOrEmpty()) {
            throw WebApplicationMapperException("Missing tag name parameter", Response.Status.BAD_REQUEST.statusCode)
        }

        return params.tag.normalizeHashtag()
    }

    private fun authorizedList(account: AccessToken, params: StreamParams): String {
        if (params.list.isNullOrEmpty()) {
            throw WebApplicationMapperException("Missing list name parameter", Response.Status.BAD_REQUEST.statusCode)
        }

        if (params.list.toLongOrNull()?.let { listService.isOwnedBy(it, account.accountId) } != true) {
            throw WebApplicationMapperException("Not authorized to stream this list", Response.Status.UNAUTHORIZED.statusCode)
        }

        return params.list
    }
}
