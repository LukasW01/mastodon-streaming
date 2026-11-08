package dev.mastodon.streaming.service

import dev.mastodon.streaming.db.account.AccountService
import dev.mastodon.streaming.db.filter.FilterService
import dev.mastodon.streaming.db.moderation.BlockService
import dev.mastodon.streaming.dto.CachedFilter
import dev.mastodon.streaming.dto.ChannelResolution
import dev.mastodon.streaming.dto.FilterResult
import dev.mastodon.streaming.dto.StatusPayload
import dev.mastodon.streaming.util.session.StreamSession

import com.fasterxml.jackson.databind.JsonNode
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

    /** @return the payload to transmit to the client, or null if the status has to be filtered out */
    @ActivateRequestContext
    fun filter(
        session: StreamSession,
        resolution: ChannelResolution,
        payload: JsonNode
    ): JsonNode? {
        val status = StatusPayload(payload)

        return when {
            isFilteredByFeedSettings(status, resolution) -> null
            isFilteredByLanguage(session, status) -> null
            // Filter based on domain blocks, blocks, mutes, or custom filters:
            isBlocked(session, status) -> null
            status.isFiltered -> payload
            else -> status.withFilterResults(applyFilters(session, status), objectMapper)
        }
    }

    private fun isFilteredByFeedSettings(status: StatusPayload, resolution: ChannelResolution): Boolean {
        val filtered = if (status.isLocal) resolution.filterLocal else resolution.filterRemote
        if (filtered) {
            Log.debugf("Message %s filtered by feed settings", status.id)
        }
        return filtered
    }

    // Filter based on language:
    private fun isFilteredByLanguage(session: StreamSession, status: StatusPayload): Boolean {
        val chosenLanguages = session.account.chosenLanguages ?: return false
        val filtered = status.language !in chosenLanguages
        if (filtered) {
            Log.debugf("Message %s filtered by language (%s)", status.id, status.language)
        }
        return filtered
    }

    // Handling blocks & mutes and domain blocks: If one of those applies,
    // then we don't transmit the payload of the event to the client
    private fun isBlocked(session: StreamSession, status: StatusPayload): Boolean {
        val accountId = session.account.accountId

        return blockService.isBlockedOrMutedBidirectional(accountId, status.authorId, status.targetAccountIds) ||
                status.accountDomain?.let { accountService.hasAccountDomainBlock(accountId, it) } == true
    }

    /** Apply cachedFilters against the payload, constructing a `filter_results` array of FilterResult entities */
    private fun applyFilters(session: StreamSession, status: StatusPayload): List<FilterResult> {
        val searchableTextContent = status.searchableText()

        // Just in-case Jsoup fails to find text in searchableContent
        if (searchableTextContent.isEmpty()) {
            return emptyList()
        }

        val now = LocalDateTime.now(ZoneOffset.UTC)

        return cachedFilters(session).values
            // Check the filter hasn't expired before applying:
            .filterNot { it.isExpired(now) }
            .mapNotNull { it.match(searchableTextContent) }
    }

    // Handling for constructing the custom filters and caching them on the session
    private fun cachedFilters(session: StreamSession): Map<Long, CachedFilter> =
        session.cachedFiltersRef.get() ?: CachedFilter.fromKeywords(filterService.findFilterKeywords(session.account.accountId).orEmpty())
            .also { session.cachedFiltersRef.set(it) }
}
