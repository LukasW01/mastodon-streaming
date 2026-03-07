package dev.mastodon.streaming.db.filter

import dev.mastodon.streaming.dto.FilterKeyword
import io.quarkus.hibernate.orm.panache.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class CustomFilterKeywordRepository : PanacheRepository<CustomFilterKeyword> {
    fun findFilterKeywords(accountId: Long): List<FilterKeyword>? = find(
        """
                select  f.id, f.phrase, f.context, f.expiresAt, f.action, k.keyword, k.wholeWord
                from CustomFilterKeyword k
                join k.filter f
                where f.account.id = ?1
                and (f.expiresAt is null or f.expiresAt > CURRENT_TIMESTAMP)
                """,
        accountId
    )
        .project(FilterKeyword::class.java)
        .list()
}
