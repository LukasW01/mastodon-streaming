package dev.mastodon.streaming.db.moderation

import io.quarkus.hibernate.orm.panache.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class BlockRepository : PanacheRepository<Block> {
    // Viewer blocks/mutes any target, OR the post author blocks the viewer.
    fun isBlockedOrMutedBidirectional(
        viewerId: Long,
        authorId: Long,
        targetIds: List<Long>
    ): Boolean =
        find(
            """
            select case when (
                exists (
                    select 1 from Block b
                    where b.account.id = ?1
                      and b.targetAccount.id in ?3
                )
                or exists (
                    select 1 from Block bo
                    where bo.account.id = ?2
                      and bo.targetAccount.id = ?1
                )
                or exists (
                    select 1 from Mute m
                    where m.account.id = ?1
                      and m.targetAccount.id in ?3
                )
            ) then true else false end
            from Account a
            where a.id = ?1
            """,
            viewerId, authorId, targetIds
        )
            .project(Boolean::class.java)
            .firstResult() ?: false
}
