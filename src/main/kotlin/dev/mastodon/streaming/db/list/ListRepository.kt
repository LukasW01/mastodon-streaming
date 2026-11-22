package dev.mastodon.streaming.db.list

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class ListRepository : PanacheRepository<List> {
    fun isOwnedBy(id: Long, accountId: Long): Boolean = count("id = ?1 and account.id = ?2", id, accountId) > 0
}
