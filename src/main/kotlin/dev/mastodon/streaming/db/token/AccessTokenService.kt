package dev.mastodon.streaming.db.token

import dev.mastodon.streaming.dto.AccessToken

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

@ApplicationScoped
class AccessTokenService {
    @Inject
    private lateinit var tokenRepository: AccessTokenRepository
    
    fun findTokenDetailsDto(token: String): AccessToken? = tokenRepository.findTokenDetailsDto(token)
}
