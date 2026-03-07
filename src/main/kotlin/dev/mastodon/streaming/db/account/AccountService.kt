package dev.mastodon.streaming.db.account

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

@ApplicationScoped
class AccountService {
    @Inject
    private lateinit var accountDomainBlockRepository: AccountDomainBlockRepository
    
    fun hasAccountDomainBlock(accountId: String, domain: String): Boolean = accountDomainBlockRepository.hasAccountDomainBlock(accountId, domain)
}
