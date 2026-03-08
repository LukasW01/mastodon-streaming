package dev.mastodon.streaming.db.moderation

import io.quarkus.hibernate.orm.panache.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class BlockRepository : PanacheRepository<Block> {
    fun isBlockedOrMuted(accountId: Long, targetIds: List<Long>): Boolean =
        find(
            """
        select
            case when (
                exists (
                    select 1
                    from Block b
                    where b.account.id = ?1
                    and b.targetAccount.id in ?2
                )
                or
                exists (
                    select 1
                    from Mute m
                    where m.account.id = ?1
                    and m.targetAccount.id in ?2
                )
            ) then true else false end
        from Block b
        """,
            accountId,
            targetIds
        )
            .project(Boolean::class.java)
            .firstResult() ?: false
}
