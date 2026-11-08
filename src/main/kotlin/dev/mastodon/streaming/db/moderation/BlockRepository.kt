package dev.mastodon.streaming.db.moderation

import io.quarkus.hibernate.orm.panache.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class BlockRepository : PanacheRepository<Block> {
    // Checks viewer blocks/mutes targets AND whether the post author blocks the viewer.
    fun isBlockedOrMutedBidirectional(
        viewerId: Long,
        authorId: Long,
        targetIds: List<Long>
    ): Boolean =
        getEntityManager().createQuery(
            """
            select 1
            from Block b
            where (b.account.id = :viewerId and b.targetAccount.id in :targetIds)
               or (b.account.id = :authorId and b.targetAccount.id = :viewerId)
            union
            select 1
            from Mute m
            where m.account.id = :viewerId
            and m.targetAccount.id in :targetIds
            """
        )
            .setParameter("viewerId", viewerId)
            .setParameter("authorId", authorId)
            .setParameter("targetIds", targetIds)
            .resultList
            .isNotEmpty()
}
