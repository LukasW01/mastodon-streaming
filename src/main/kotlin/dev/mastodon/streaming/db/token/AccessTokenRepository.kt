package dev.mastodon.streaming.db.token

import dev.mastodon.streaming.dto.AccessToken
import io.quarkus.hibernate.orm.panache.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class AccessTokenRepository : PanacheRepository<OauthAccessToken> {
    fun findTokenDetailsDto(token: String): AccessToken? = find(
        """
            SELECT t.id, u.id, a.id, u.chosenLanguages, t.scopes, CAST(COALESCE(r.permissions, 0) AS integer)
            FROM OauthAccessToken t
            JOIN t.resourceOwner u
            JOIN u.account a
            LEFT JOIN u.role r
            WHERE t.token = ?1 AND t.revokedAt IS NULL AND u.disabled = FALSE AND a.suspendedAt IS NULL
            """,
        token
    )
        .project(AccessToken::class.java)
        .firstResult()
}
