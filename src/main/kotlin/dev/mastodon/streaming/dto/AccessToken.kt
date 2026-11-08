package dev.mastodon.streaming.dto

import dev.mastodon.streaming.util.ChannelName
import dev.mastodon.streaming.util.mapper.WebApplicationMapperException
import io.quarkus.runtime.annotations.RegisterForReflection
import jakarta.json.bind.annotation.JsonbCreator
import jakarta.ws.rs.core.Response

/** The account resolved from an access token, Hibernate fills this from the `findTokenDetailsDto` projection. */
@RegisterForReflection
data class AccessToken @JsonbCreator constructor(
    val tokenId: Long,
    val resourceOwnerId: Long,
    val accountId: Long,
    val chosenLanguages: List<String>? = null,
    val scopes: String?,
    val permissions: Long,
) {
    fun checkScopes(channelName: ChannelName?): AccessToken {
        // The `read` scope has the highest priority, if the token has it
        // then it can access all streams
        val scopes = mutableListOf("read")

        // When accessing specifically the notifications stream,
        // we need a read:notifications, while in all other cases,
        // we can allow access with read:statuses. Mind that the
        // user stream will not contain notifications unless
        // the token has either read or read:notifications scope
        // as well, this is handled separately.
        scopes += if (channelName == ChannelName.USER_NOTIFICATION) "read:notifications" else "read:statuses"

        if (!isInScope(scopes)) {
            throw WebApplicationMapperException("Access token does not have the required scopes", Response.Status.UNAUTHORIZED.statusCode)
        }
        
        return this
    }

    fun isInScope(necessaryScopes: List<String>): Boolean = scopes.orEmpty().split(' ').any { it in necessaryScopes }
}
