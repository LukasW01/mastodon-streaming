package dev.mastodon.streaming.db.moderation

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

@ApplicationScoped
class BlockService {
    @Inject
    private lateinit var blockRepository: BlockRepository

    fun isBlockedOrMutedBidirectional(
        viewerId: Long,
        authorId: Long,
        targetIds: List<Long>
    ): Boolean =
        blockRepository.isBlockedOrMutedBidirectional(viewerId, authorId, targetIds)
}
