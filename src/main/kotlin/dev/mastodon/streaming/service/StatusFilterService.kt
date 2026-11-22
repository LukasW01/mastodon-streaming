package dev.mastodon.streaming.service

import dev.mastodon.streaming.db.account.AccountService
import dev.mastodon.streaming.db.filter.FilterService
import dev.mastodon.streaming.db.moderation.BlockService
import dev.mastodon.streaming.dto.ChannelResolution
import dev.mastodon.streaming.dto.FilterResult
import dev.mastodon.streaming.dto.FilteredStatus
import dev.mastodon.streaming.dto.RedisMessage
import dev.mastodon.streaming.dto.StatusPayload
import dev.mastodon.streaming.dto.UserCustomFilter
import dev.mastodon.streaming.util.session.StreamSession

import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.context.control.ActivateRequestContext
import jakarta.inject.Inject

import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Handles the filtering of statuses: Majority of the filtering happens on the
 * Ruby on Rails side when producing the event for streaming, this only covers
 * what depends on the client that is streaming.
 */
@ApplicationScoped
class StatusFilterService {
    @Inject
    private lateinit var blockService: BlockService

    @Inject
    private lateinit var accountService: AccountService

    @Inject
    private lateinit var filterService: FilterService

    @Inject
    private lateinit var objectMapper: ObjectMapper

    /** @return the encoded payload to transmit to the client, or null if the status has to be filtered out */
    @ActivateRequestContext
    fun filter(
        session: StreamSession,
        resolution: ChannelResolution,
        message: RedisMessage
    ): String? {
        message.payload ?: return null

        objectMapper.treeToValue(message.payload, StatusPayload::class.java).let { status ->
            return when {
                isFilteredByFeedSettings(status, resolution) -> null
                isFilteredByLanguage(session, status) -> null
                // Filter based on domain blocks, blocks, mutes, or custom filters:
                isBlocked(session, status) -> null
                status.isFiltered() -> message.encodedPayload
                else -> objectMapper.writeValueAsString(FilteredStatus(message.payload, applyFilters(session, status)))
            }
        }
    }

    private fun isFilteredByFeedSettings(status: StatusPayload, resolution: ChannelResolution): Boolean =
        (if (status.isLocal()) resolution.filterLocal else resolution.filterRemote).also { filtered ->
            if (filtered) {
                Log.debugf("Message %s filtered by feed settings", status.id)
            }
        }

    private fun isFilteredByLanguage(session: StreamSession, status: StatusPayload): Boolean =
        (session.account.chosenLanguages?.let { status.language !in it } == true).also { filtered ->
            if (filtered) {
                Log.debugf("Message %s filtered by language (%s)", status.id, status.language)
            }
        }

    // Handling blocks & mutes and domain blocks: If one of those applies,
    // then we don't transmit the payload of the event to the client
    private fun isBlocked(session: StreamSession, status: StatusPayload): Boolean =
        blockService.isBlockedOrMutedBidirectional(session.account.accountId, status.authorId(), status.targetAccountIds()) ||
                status.accountDomain()?.let { accountService.hasAccountDomainBlock(session.account.accountId, it) } == true

    /** Apply the custom filters of the account against the payload, constructing a `filtered` array of FilterResult entities */
    private fun applyFilters(session: StreamSession, status: StatusPayload): List<FilterResult> =
        // Just in-case Jsoup fails to find text in searchableContent
        status.searchableText()
            .takeIf { it.isNotEmpty() }
            ?.let { textContent ->
                UserCustomFilter.fromKeywords(filterService.findFilterKeywords(session.account.accountId).orEmpty())
                    .values
                    // Check the filter hasn't expired before applying:
                    .filterNot { it.isExpired(LocalDateTime.now(ZoneOffset.UTC)) }
                    .mapNotNull { it.match(textContent) }
            }
            .orEmpty()
}
