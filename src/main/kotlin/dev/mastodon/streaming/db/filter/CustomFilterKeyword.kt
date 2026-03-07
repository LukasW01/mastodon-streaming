package dev.mastodon.streaming.db.filter

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
    name = "custom_filter_keywords",
    indexes = [Index(
        name = "index_custom_filter_keywords_on_custom_filter_id",
        columnList = "custom_filter_id"
    )]
)
class CustomFilterKeyword : PanacheEntityBase() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long = 0

    @NotNull
    @ColumnDefault("true")
    @Column(name = "whole_word")
    var wholeWord: Boolean? = null

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = CustomFilter::class)
    @JoinColumn(name = "custom_filter_id")
    lateinit var filter: CustomFilter

    @NotBlank
    @ColumnDefault("''")
    @Column(name = "keyword", length = Integer.MAX_VALUE)
    lateinit var keyword: String
    
    @Column(name = "created_at")
    lateinit var createdAt: LocalDateTime

    @Column(name = "updated_at")
    lateinit var updatedAt: LocalDateTime
}
