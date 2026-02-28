package dev.mastodon.streaming.db.token

import dev.mastodon.streaming.db.user.User
import io.quarkus.hibernate.orm.panache.PanacheEntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.net.InetAddress
import java.time.LocalDateTime

@Entity
@Table(
    name = "oauth_access_tokens", indexes = [
        Index(
            name = "index_oauth_access_tokens_on_token",
            columnList = "token",
            unique = true
        ),
        Index(
            name = "index_oauth_access_tokens_on_refresh_token",
            columnList = "refresh_token",
            unique = true
        ),
        Index(
            name = "index_oauth_access_tokens_on_resource_owner_id",
            columnList = "resource_owner_id"
        )]
)
class OauthAccessToken : PanacheEntityBase() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long = 0

    @Column(name = "refresh_token", length = Integer.MAX_VALUE)
    var refreshToken: String? = null

    @Column(name = "expires_in")
    var expiresIn: Int? = null

    @Column(name = "revoked_at")
    var revokedAt: LocalDateTime? = null

    @Column(name = "scopes", length = Integer.MAX_VALUE)
    var scopes: String? = null

    @Column(name = "application_id")
    var applicationId: Long? = null
    
    @NotBlank
    @Column(name = "token", length = Integer.MAX_VALUE)
    lateinit var token: String

    @Column(name = "created_at")
    lateinit var createdAt: LocalDateTime

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = User::class)
    @JoinColumn(name = "resource_owner_id")
    lateinit var resourceOwner: User

    @Column(name = "last_used_at")
    lateinit var lastUsedAt: LocalDateTime

    @Column(name = "last_used_ip")
    lateinit var lastUsedIp: InetAddress
}
