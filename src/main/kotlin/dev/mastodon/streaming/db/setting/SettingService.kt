package dev.mastodon.streaming.db.setting

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import kotlin.collections.List

@ApplicationScoped
class SettingService {
    @Inject
    private lateinit var settingRepository: SettingRepository

    fun findByKind(kind: String): List<Setting> = if (kind == "hashtag") {
        settingRepository.findByVariable(listOf("local_topic_feed_access", "remote_topic_feed_access"))
    } else {
        settingRepository.findByVariable(listOf("local_live_feed_access", "remote_live_feed_access"))
    }
}
