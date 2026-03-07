package dev.mastodon.streaming.db.filter

import dev.mastodon.streaming.db.account.Account
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
import org.hibernate.annotations.ColumnDefault
import java.time.LocalDateTime

@Entity
@Table(
    name = "custom_filters",
    indexes = [Index(
        name = "index_custom_filters_on_account_id",
        columnList = "account_id"
    )]
)
class CustomFilter : PanacheEntityBase() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long = 0

    @NotNull
    @ColumnDefault("0")
    @Column(name = "action")
    var action: Int? = null

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Account::class)
    @JoinColumn(name = "account_id")
    lateinit var account: Account

    @Column(name = "expires_at")
    lateinit var expiresAt: LocalDateTime

    @NotBlank
    @ColumnDefault("''")
    @Column(name = "phrase", length = Integer.MAX_VALUE)
    lateinit var phrase: String

    @NotNull
    @ColumnDefault("'{}'")
    @Column(name = "context")
    lateinit var context: List<String>
    
    @Column(name = "created_at")
    lateinit var createdAt: LocalDateTime

    @Column(name = "updated_at")
    lateinit var updatedAt: LocalDateTime
}
