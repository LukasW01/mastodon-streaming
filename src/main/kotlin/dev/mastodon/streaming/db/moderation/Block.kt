package dev.mastodon.streaming.db.moderation

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
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

@Entity
@Table(
    name = "blocks", indexes = [
        Index(
            name = "index_blocks_on_account_id_and_target_account_id",
            columnList = "account_id, target_account_id",
            unique = true
        ),
        Index(
            name = "index_blocks_on_target_account_id",
            columnList = "target_account_id"
        )]
)
class Block : PanacheEntityBase() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long = 0
    
    @Column(name = "uri", length = Integer.MAX_VALUE)
    var uri: String? = null

    @Column(name = "created_at")
    lateinit var createdAt: LocalDateTime

    @Column(name = "updated_at")
    lateinit var updatedAt: LocalDateTime

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Account::class)
    @JoinColumn(name = "account_id")
    lateinit var account: Account

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Account::class)
    @JoinColumn(name = "target_account_id")
    lateinit var targetAccount: Account
}
