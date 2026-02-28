package dev.mastodon.streaming.db.setting

import io.quarkus.hibernate.orm.panache.PanacheEntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

@Entity
@Table(
    name = "settings", indexes = [Index(
        name = "index_settings_on_var",
        columnList = "var",
        unique = true
    )]
)
class Setting : PanacheEntityBase() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long = 0

    @NotBlank
    @Column(name = "var", length = Integer.MAX_VALUE)
    lateinit var variable: String

    @NotBlank
    @Column(name = "value", length = Integer.MAX_VALUE)
    lateinit var value: String

    @Column(name = "created_at")
    lateinit var createdAt: LocalDateTime

    @Column(name = "updated_at")
    lateinit var updatedAt: LocalDateTime
}
