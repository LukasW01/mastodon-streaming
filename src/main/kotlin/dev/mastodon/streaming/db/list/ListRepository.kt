package dev.mastodon.streaming.db.list

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class ListRepository : PanacheRepository<List> {
    fun findByListIdAndAccountId(id: Long, accountId: Long): List? = find("id = ?1 and account.id = ?2", id, accountId).firstResult()
}
