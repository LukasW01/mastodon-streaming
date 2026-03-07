package dev.mastodon.streaming.db.account

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class AccountDomainBlockRepository : PanacheRepository<AccountDomainBlock> {
    fun hasAccountDomainBlock(accountId: String, domain: String): Boolean = count("account.id = ?1 and domain = ?2", accountId, domain) > 0
}
