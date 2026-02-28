package dev.mastodon.streaming.db.setting

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped
import kotlin.collections.List

@ApplicationScoped
class SettingRepository : PanacheRepository<Setting> {
    fun findByVariable(variable: List<String>): List<Setting> = find("variable in ?1", variable).list()
}
