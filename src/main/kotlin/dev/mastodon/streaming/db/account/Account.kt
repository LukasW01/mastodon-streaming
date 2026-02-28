package dev.mastodon.streaming.db.account

import io.quarkus.hibernate.orm.panache.PanacheEntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime
import kotlin.collections.List

@Entity
@Table(
    name = "accounts", indexes = [
        Index(
            name = "index_accounts_on_domain_and_id",
            columnList = "domain, id"
        ),
        Index(
            name = "index_accounts_on_uri",
            columnList = "uri"
        ),
        Index(
            name = "index_accounts_on_url",
            columnList = "url"
        ),
        Index(
            name = "index_accounts_on_moved_to_account_id",
            columnList = "moved_to_account_id"
        )]
)
class Account : PanacheEntityBase() {
    @Id
    @ColumnDefault("timestamp_id('accounts')")
    @Column(name = "id")
    var id: Long = 0

    @Column(name = "domain", length = Integer.MAX_VALUE)
    var domain: String? = null

    @Column(name = "private_key", length = Integer.MAX_VALUE)
    var privateKey: String? = null

    @Column(name = "url", length = Integer.MAX_VALUE)
    var url: String? = null

    @Column(name = "avatar_file_name", length = Integer.MAX_VALUE)
    var avatarFileName: String? = null

    @Column(name = "avatar_content_type", length = Integer.MAX_VALUE)
    var avatarContentType: String? = null

    @Column(name = "avatar_file_size")
    var avatarFileSize: Int? = null

    @Column(name = "avatar_updated_at")
    var avatarUpdatedAt: LocalDateTime? = null

    @Column(name = "header_file_name", length = Integer.MAX_VALUE)
    var headerFileName: String? = null

    @Column(name = "header_content_type", length = Integer.MAX_VALUE)
    var headerContentType: String? = null

    @Column(name = "header_file_size")
    var headerFileSize: Int? = null

    @Column(name = "header_updated_at")
    var headerUpdatedAt: LocalDateTime? = null

    @Column(name = "avatar_remote_url", length = Integer.MAX_VALUE)
    var avatarRemoteUrl: String? = null

    @NotNull
    @ColumnDefault("false")
    @Column(name = "locked")
    var locked: Boolean? = null

    @ColumnDefault("''")
    @Column(name = "header_remote_url", length = Integer.MAX_VALUE)
    var headerRemoteUrl: String? = null

    @Column(name = "last_webfingered_at")
    var lastWebfingeredAt: LocalDateTime? = null

    @ColumnDefault("''")
    @Column(name = "inbox_url", length = Integer.MAX_VALUE)
    var inboxUrl: String? = null

    @ColumnDefault("''")
    @Column(name = "outbox_url", length = Integer.MAX_VALUE)
    var outboxUrl: String? = null

    @ColumnDefault("''")
    @Column(name = "shared_inbox_url", length = Integer.MAX_VALUE)
    var sharedInboxUrl: String? = null

    @ColumnDefault("''")
    @Column(name = "followers_url", length = Integer.MAX_VALUE)
    var followersUrl: String? = null

    @NotNull
    @ColumnDefault("0")
    @Column(name = "protocol")
    var protocol: Int? = null

    @NotNull
    @ColumnDefault("false")
    @Column(name = "memorial")
    var memorial: Boolean? = null

    @Column(name = "moved_to_account_id")
    var movedToAccountId: Long? = null

    @Column(name = "featured_collection_url", length = Integer.MAX_VALUE)
    var featuredCollectionUrl: String? = null

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fields")
    var fields: List<Map<String, Any>>? = null

    @Column(name = "actor_type", length = Integer.MAX_VALUE)
    var actorType: String? = null

    @Column(name = "discoverable")
    var discoverable: Boolean? = null

    @Column(name = "also_known_as")
    var alsoKnownAs: List<String>? = null

    @Column(name = "silenced_at")
    var silencedAt: LocalDateTime? = null

    @Column(name = "suspended_at")
    var suspendedAt: LocalDateTime? = null

    @Column(name = "hide_collections")
    var hideCollections: Boolean? = null

    @Column(name = "avatar_storage_schema_version")
    var avatarStorageSchemaVersion: Int? = null

    @Column(name = "header_storage_schema_version")
    var headerStorageSchemaVersion: Int? = null

    @Column(name = "suspension_origin")
    var suspensionOrigin: Int? = null

    @Column(name = "sensitized_at")
    var sensitizedAt: LocalDateTime? = null

    @Column(name = "trendable")
    var trendable: Boolean? = null

    @Column(name = "reviewed_at")
    var reviewedAt: LocalDateTime? = null

    @Column(name = "requested_review_at")
    var requestedReviewAt: LocalDateTime? = null

    @NotNull
    @ColumnDefault("false")
    @Column(name = "indexable")
    var indexable: Boolean? = null

    @ColumnDefault("'{}'")
    @Column(name = "attribution_domains")
    var attributionDomains: List<String>? = null

    @ColumnDefault("1")
    @Column(name = "id_scheme")
    var idScheme: Int? = null

    @NotBlank
    @ColumnDefault("''")
    @Column(name = "username", length = Integer.MAX_VALUE)
    lateinit var username: String

    @NotBlank
    @ColumnDefault("''")
    @Column(name = "public_key", length = Integer.MAX_VALUE)
    lateinit var publicKey: String

    @Column(name = "created_at")
    lateinit var createdAt: LocalDateTime

    @Column(name = "updated_at")
    lateinit var updatedAt: LocalDateTime

    @NotBlank
    @ColumnDefault("''")
    @Column(name = "note", length = Integer.MAX_VALUE)
    lateinit var note: String

    @NotBlank
    @ColumnDefault("''")
    @Column(name = "display_name", length = Integer.MAX_VALUE)
    lateinit var displayName: String

    @NotBlank
    @ColumnDefault("''")
    @Column(name = "uri", length = Integer.MAX_VALUE)
    lateinit var uri: String

    @NotBlank
    @ColumnDefault("''")
    @Column(name = "following_url", length = Integer.MAX_VALUE)
    lateinit var followingUrl: String
}
