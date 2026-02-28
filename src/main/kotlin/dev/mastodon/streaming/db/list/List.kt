package dev.mastodon.streaming.db.list

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
    name = "lists", indexes = [Index(
        name = "index_lists_on_account_id",
        columnList = "account_id"
    )]
)
class List : PanacheEntityBase() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long = 0
    
    @NotNull
    @ColumnDefault("0")
    @Column(name = "replies_policy")
    var repliesPolicy: Int = 0

    @NotNull
    @ColumnDefault("false")
    @Column(name = "exclusive")
    var exclusive: Boolean = false

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Account::class)
    @JoinColumn(name = "account_id")
    lateinit var account: Account

    @NotBlank
    @ColumnDefault("''")
    @Column(name = "title", length = Integer.MAX_VALUE)
    lateinit var title: String

    @Column(name = "created_at")
    lateinit var createdAt: LocalDateTime

    @Column(name = "updated_at")
    lateinit var updatedAt: LocalDateTime
}
