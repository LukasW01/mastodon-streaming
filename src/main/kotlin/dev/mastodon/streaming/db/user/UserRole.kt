package dev.mastodon.streaming.db.user

import io.quarkus.hibernate.orm.panache.PanacheEntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.ColumnDefault
import java.time.LocalDateTime

@Entity
@Table(name = "user_roles")
class UserRole : PanacheEntityBase() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long = 0

    @NotNull
    @ColumnDefault("0")
    @Column(name = "position")
    var position: Int? = null

    @NotNull
    @ColumnDefault("0")
    @Column(name = "permissions")
    var permissions: Long? = null

    @NotNull
    @ColumnDefault("false")
    @Column(name = "highlighted")
    var highlighted: Boolean? = null

    @NotBlank
    @ColumnDefault("''")
    @Column(name = "name", length = Integer.MAX_VALUE)
    lateinit var name: String

    @NotBlank
    @ColumnDefault("''")
    @Column(name = "color", length = Integer.MAX_VALUE)
    lateinit var color: String

    @Column(name = "created_at")
    lateinit var createdAt: LocalDateTime

    @Column(name = "updated_at")
    lateinit var updatedAt: LocalDateTime
}
