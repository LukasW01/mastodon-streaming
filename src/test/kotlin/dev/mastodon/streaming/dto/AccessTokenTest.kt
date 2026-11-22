package dev.mastodon.streaming.dto

import dev.mastodon.streaming.util.ChannelName
import dev.mastodon.streaming.util.mapper.WebApplicationMapperException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class AccessTokenTest {
    private val token = AccessToken(tokenId = 10, resourceOwnerId = 20, accountId = 30, scopes = "read", permissions = 0)

    @Test
    fun `isInScope matches any of the space separated scopes`() {
        assertTrue(token.copy(scopes = "write read:statuses").isInScope(listOf("read", "read:statuses")))
    }

    @Test
    fun `isInScope only matches whole scopes`() {
        assertFalse(token.copy(scopes = "read:statuses").isInScope(listOf("read")))
    }

    @Test
    fun `isInScope is false without scopes`() {
        assertFalse(token.copy(scopes = null).isInScope(listOf("read")))
        assertFalse(token.copy(scopes = "").isInScope(listOf("read")))
    }

    @ParameterizedTest
    @EnumSource(ChannelName::class)
    fun readScopeCanAccessEveryStream(channel: ChannelName) {
        assertSame(token, token.checkScopes(channel))
    }

    @Test
    fun `read statuses can access every stream but notifications`() {
        val readStatuses = token.copy(scopes = "read:statuses")

        assertSame(readStatuses, readStatuses.checkScopes(ChannelName.PUBLIC))
        assertSame(readStatuses, readStatuses.checkScopes(ChannelName.USER))
        assertEquals(401, assertThrows<WebApplicationMapperException> { readStatuses.checkScopes(ChannelName.USER_NOTIFICATION) }.status)
    }

    @Test
    fun `read notifications can only access the notification stream`() {
        val readNotifications = token.copy(scopes = "read:notifications")

        assertSame(readNotifications, readNotifications.checkScopes(ChannelName.USER_NOTIFICATION))
        assertEquals(401, assertThrows<WebApplicationMapperException> { readNotifications.checkScopes(ChannelName.PUBLIC) }.status)
    }

    @Test
    fun `tokens without read scope are rejected`() {
        val err: WebApplicationMapperException = assertThrows { token.copy(scopes = "write follow").checkScopes(null) }

        assertEquals(401, err.status)
        assertEquals("Access token does not have the required scopes", err.message)
    }
}
