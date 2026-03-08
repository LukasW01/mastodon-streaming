package dev.mastodon.streaming.db.moderation

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

@ApplicationScoped
class BlockService {
    @Inject
    private lateinit var blockRepository: BlockRepository

    fun isBlockedOrMuted(accountId: Long, targetIds: List<Long>): Boolean =
        blockRepository.isBlockedOrMuted(accountId, targetIds)
}
