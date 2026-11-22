package dev.mastodon.streaming.dto

import com.fasterxml.jackson.databind.ObjectMapper

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StatusPayloadTest {
    private val objectMapper = ObjectMapper().findAndRegisterModules()
    private val local = StatusPayload(account = StatusPayload.Account(id = 2, username = "alice", acct = "alice"))
    private val remote = StatusPayload(account = StatusPayload.Account(id = 3, username = "bob", acct = "bob@remote.example"))

    @Test
    fun `statuses of local accounts are local`() {
        assertTrue(local.isLocal())
        assertFalse(remote.isLocal())
    }

    @Test
    fun `the account domain is only known for remote accounts`() {
        assertNull(local.accountDomain())
        assertEquals("remote.example", remote.accountDomain())
    }

    @Test
    fun `the targets are the author and the mentioned accounts`() {
        val status = local.copy(mentions = listOf(StatusPayload.Mention(4), StatusPayload.Mention(5)))

        assertEquals(2L, status.authorId())
        assertEquals(listOf(2L, 4L, 5L), status.targetAccountIds())
    }

    @Test
    fun `statuses without account have no author`() {
        assertEquals(0L, StatusPayload().authorId())
        assertEquals(listOf(0L), StatusPayload().targetAccountIds())
    }

    @Test
    fun `searchable text contains all text of the status without html`() {
        val status = StatusPayload(
            spoilerText = "CW",
            content = "<p>Hello<br>World</p><p>Second &amp; last</p>",
            poll = StatusPayload.Poll(listOf(StatusPayload.PollOption("Yes"), StatusPayload.PollOption("No"))),
            mediaAttachments = listOf(StatusPayload.MediaAttachment("A cat"), StatusPayload.MediaAttachment()),
        )

        assertEquals("CW\n\nHello\nWorld\n\nSecond & last\n\nYes\n\nNo\n\nA cat\n\n", status.searchableText())
    }

    @Test
    fun `statuses are deserialized from the payload of mastodon`() {
        val status = objectMapper.readValue(
            """
            {
              "id": "1",
              "language": "en",
              "account": {"id": 2, "username": "bob", "acct": "bob@remote.example", "display_name": "Bob"},
              "mentions": [{"id": 3, "acct": "alice"}],
              "poll": {"options": [{"title": "Yes", "votes_count": 1}]},
              "media_attachments": [{"description": "A cat", "type": "image"}],
              "spoiler_text": "CW",
              "content": "<p>Hello</p>",
              "reblogs_count": 0
            }
            """,
            StatusPayload::class.java,
        )

        assertEquals("1", status.id)
        assertEquals("en", status.language)
        assertEquals(StatusPayload.Account(2, "bob", "bob@remote.example"), status.account)
        assertEquals(listOf(2L, 3L), status.targetAccountIds())
        assertEquals("CW\n\nHello\n\nYes\n\nA cat", status.searchableText())
        assertFalse(status.isFiltered())
    }

    @Test
    fun `statuses with filter results were already filtered by mastodon`() {
        assertTrue(objectMapper.readValue("""{"id": "1", "filtered": []}""", StatusPayload::class.java).isFiltered())
    }
}
