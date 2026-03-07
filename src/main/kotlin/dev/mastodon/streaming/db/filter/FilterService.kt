package dev.mastodon.streaming.db.filter

import dev.mastodon.streaming.dto.FilterKeyword
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

@ApplicationScoped
class FilterService {
    @Inject
    private lateinit var customFilterKeywordRepository: CustomFilterKeywordRepository
    
    fun findFilterKeywords(accountId: Long): List<FilterKeyword>? = customFilterKeywordRepository.findFilterKeywords(accountId)
}
