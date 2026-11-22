package dev.mastodon.streaming.service

import dev.mastodon.streaming.FakeSession
import dev.mastodon.streaming.db.account.AccountService
import dev.mastodon.streaming.db.filter.FilterService
import dev.mastodon.streaming.db.moderation.BlockService
import dev.mastodon.streaming.dto.AccessToken
import dev.mastodon.streaming.dto.ChannelResolution
import dev.mastodon.streaming.dto.FilterKeyword
import dev.mastodon.streaming.dto.RedisMessage
import dev.mastodon.streaming.dto.StreamParams
import dev.mastodon.streaming.util.ChannelName

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

import java.time.LocalDateTime
import java.time.ZoneOffset

private const val LOCAL_STATUS = """
    {
      "id": "1",
      "language": "en",
      "account": {"id": 2, "username": "alice", "acct": "alice"},
      "mentions": [{"id": 3}],
      "content": "<p>Kotlin</p>"
    }
"""

private const val REMOTE_STATUS = """
    {
      "id": "1",
      "language": "en",
      "account": {"id": 2, "username": "bob", "acct": "bob@remote.example"},
      "content": "<p>Hello</p>"
    }
"""

@ExtendWith(MockitoExtension::class)
class StatusFilterServiceTest {
    private val account = AccessToken(tokenId = 10, resourceOwnerId = 20, accountId = 30, scopes = "read", permissions = 0)
    private val session = FakeSession(account)
    private val resolution = ChannelResolution(ChannelName.PUBLIC, StreamParams(), listOf("timeline:public"), needsFiltering = true)
    private val keyword = FilterKeyword(7, "Languages", listOf("home", "public"), null, 0, "kotlin", false)

    @Spy
    private val objectMapper: ObjectMapper = ObjectMapper().findAndRegisterModules()

    @Mock
    private lateinit var blockService: BlockService

    @Mock
    private lateinit var accountService: AccountService

    @Mock
    private lateinit var filterService: FilterService

    @InjectMocks
    private lateinit var statusFilterService: StatusFilterService

    @Test
    fun `messages without payload are dropped`() {
        assertNull(statusFilterService.filter(session, resolution, RedisMessage("update")))
        verifyNoInteractions(blockService, accountService, filterService)
    }

    @Test
    fun `local statuses are dropped when the local feed is disabled`() {
        assertNull(statusFilterService.filter(session, resolution.copy(filterLocal = true), RedisMessage("update", objectMapper.readTree(LOCAL_STATUS))))
        assertNotNull(statusFilterService.filter(session, resolution.copy(filterLocal = true), RedisMessage("update", objectMapper.readTree(REMOTE_STATUS))))
    }

    @Test
    fun `remote statuses are dropped when the remote feed is disabled`() {
        assertNull(statusFilterService.filter(session, resolution.copy(filterRemote = true), RedisMessage("update", objectMapper.readTree(REMOTE_STATUS))))
        assertNotNull(statusFilterService.filter(session, resolution.copy(filterRemote = true), RedisMessage("update", objectMapper.readTree(LOCAL_STATUS))))
    }

    @Test
    fun `statuses in languages the account did not choose are dropped`() {
        assertNull(statusFilterService.filter(FakeSession(account.copy(chosenLanguages = listOf("de"))), resolution, RedisMessage("update", objectMapper.readTree(
            LOCAL_STATUS
        ))))
    }

    @Test
    fun `statuses in a chosen language are kept`() {
        assertNotNull(statusFilterService.filter(FakeSession(account.copy(chosenLanguages = listOf("de", "en"))), resolution, RedisMessage("update",
            objectMapper.readTree(LOCAL_STATUS))))
    }

    @Test
    fun `statuses are dropped when the author or a mentioned account is blocked or muted`() {
        whenever(blockService.isBlockedOrMutedBidirectional(30, 2, listOf(2, 3))).thenReturn(true)

        assertNull(statusFilterService.filter(session, resolution, RedisMessage("update", objectMapper.readTree(LOCAL_STATUS))))
    }

    @Test
    fun `statuses from blocked domains are dropped`() {
        whenever(accountService.hasAccountDomainBlock(30, "remote.example")).thenReturn(true)

        assertNull(statusFilterService.filter(session, resolution, RedisMessage("update", objectMapper.readTree(REMOTE_STATUS))))
    }

    @Test
    fun `domain blocks are not looked up for local statuses`() {
        statusFilterService.filter(session, resolution, RedisMessage("update", objectMapper.readTree(LOCAL_STATUS)))

        verifyNoInteractions(accountService)
    }

    @Test
    fun `statuses already filtered by mastodon are transmitted unchanged`() {
        val message = RedisMessage("update", objectMapper.readTree("""{"id": "1", "content": "<p>Kotlin</p>", "filtered": []}"""))

        assertEquals(message.encodedPayload, statusFilterService.filter(session, resolution, message))
        verifyNoInteractions(filterService)
    }

    @Test
    fun `matching custom filters are attached to the status`() {
        whenever(filterService.findFilterKeywords(30)).thenReturn(listOf(keyword))

        val result = objectMapper.readTree(statusFilterService.filter(session, resolution, RedisMessage("update", objectMapper.readTree(LOCAL_STATUS))))

        assertEquals("1", result["id"].asText())
        assertEquals("<p>Kotlin</p>", result["content"].asText())
        assertEquals(
            objectMapper.readTree(
                """
                [{
                  "filter": {"id": "7", "title": "Languages", "context": ["home", "public"], "expires_at": null, "filter_action": "warn"},
                  "keyword_matches": ["Kotlin"],
                  "status_matches": null
                }]
                """
            ),
            result["filtered"],
        )
    }

    @Test
    fun `expired custom filters are not applied`() {
        whenever(filterService.findFilterKeywords(30)).thenReturn(listOf(keyword.copy(expiresAt = LocalDateTime.now(ZoneOffset.UTC).minusDays(1))))

        assertEquals(objectMapper.createArrayNode(), objectMapper.readTree(statusFilterService.filter(session, resolution, RedisMessage("update", objectMapper.readTree(
            LOCAL_STATUS
        ))))["filtered"])
    }

    @Test
    fun `statuses without custom filters get empty filter results`() {
        whenever(filterService.findFilterKeywords(30)).thenReturn(null)

        assertEquals(objectMapper.createArrayNode(), objectMapper.readTree(statusFilterService.filter(session, resolution, RedisMessage("update", objectMapper.readTree(
            LOCAL_STATUS
        ))))["filtered"])
        verify(filterService).findFilterKeywords(30)
    }
}
