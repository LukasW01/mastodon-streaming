package dev.mastodon.streaming.db.setting

import dev.mastodon.streaming.dto.FeedSetting
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped
import kotlin.collections.List

@ApplicationScoped
class SettingRepository : PanacheRepository<Setting> {
    fun findByVariable(variable: List<String>): List<FeedSetting> =
        find("select s.variable, s.value from Setting s where s.variable in ?1", variable)
            .project(FeedSetting::class.java)
            .list()
}
