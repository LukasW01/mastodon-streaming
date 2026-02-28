package dev.mastodon.streaming.db.list

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

@ApplicationScoped
class ListService {
    @Inject
    private lateinit var listRepository: ListRepository

    fun findByListIdAndAccountId(id: Long, accountId: Long): List? = listRepository.findByListIdAndAccountId(id, accountId)
}
