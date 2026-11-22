package dev.mastodon.streaming.controller

import dev.mastodon.streaming.db.list.ListService
import dev.mastodon.streaming.db.token.AccessTokenService
import dev.mastodon.streaming.dto.AccessToken
import dev.mastodon.streaming.service.StreamService
import dev.mastodon.streaming.util.ChannelName
import dev.mastodon.streaming.util.session.ConnectionType

import io.quarkus.test.InjectMock
import io.quarkus.test.common.http.TestHTTPResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.mockito.InjectSpy
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@QuarkusTest
class EventSourceControllerTest {
    private val token = AccessToken(tokenId = 10, resourceOwnerId = 20, accountId = 30, scopes = "read", permissions = 0)

    @InjectMock
    lateinit var accessTokenService: AccessTokenService

    @InjectMock
    lateinit var listService: ListService

    @InjectSpy
    lateinit var streamService: StreamService

    @TestHTTPResource("/api/v1/streaming/")
    lateinit var streamingUri: URI

    @Test
    fun `unknown streams are rejected`() {
        given().header("Authorization", "Bearer secret")
            .`when`()
            .get("/api/v1/streaming/nonsense")
            .then()
            .statusCode(400)
            .body("error", equalTo("Unknown channel requested"))
    }

    @Test
    fun `requests without access token are rejected`() {
        given().`when`()
            .get("/api/v1/streaming/public")
            .then()
            .statusCode(401)
            .body("error", equalTo("Missing or invalid access token"))
    }

    @Test
    fun `requests with an unknown access token are rejected`() {
        given().queryParam("access_token", "revoked")
            .`when`()
            .get("/api/v1/streaming/public")
            .then()
            .statusCode(401)
            .body("error", equalTo("Missing or invalid access token"))
    }

    @Test
    fun `access tokens without the required scopes are rejected`() {
        whenever(accessTokenService.findTokenDetailsDto("secret")).thenReturn(token.copy(scopes = "read:statuses"))

        given().header("Authorization", "Bearer secret")
            .`when`()
            .get("/api/v1/streaming/user/notification")
            .then()
            .statusCode(401)
    }

    @Test
    fun `hashtag streams without tag are rejected`() {
        whenever(accessTokenService.findTokenDetailsDto("secret")).thenReturn(token)

        given().header("Authorization", "Bearer secret")
            .`when`()
            .get("/api/v1/streaming/hashtag")
            .then()
            .statusCode(400)
            .body("error", equalTo("Missing tag name parameter"))
    }

    @Test
    fun `lists of other accounts are rejected`() {
        whenever(accessTokenService.findTokenDetailsDto("secret")).thenReturn(token)
        whenever(listService.isOwnedBy(5, 30)).thenReturn(false)

        given().header("Authorization", "Bearer secret")
            .queryParam("list", "5")
            .`when`()
            .get("/api/v1/streaming/list")
            .then()
            .statusCode(401)
            .body("error", equalTo("Not authorized to stream this list"))
    }

    @Test
    fun `streams are opened with a comment and closed when the client disconnects`() {
        whenever(accessTokenService.findTokenDetailsDto("secret")).thenReturn(token)
        val request = HttpRequest.newBuilder(streamingUri.resolve("public/local?access_token=secret&only_media=true"))
            .timeout(Duration.ofSeconds(10))
            .build()

        HttpClient.newHttpClient().use { client ->
            val response = client.send(request, HttpResponse.BodyHandlers.ofLines())

            assertEquals(200, response.statusCode())
            assertEquals("text/event-stream", response.headers().firstValue("Content-Type").orElse(null))
            assertEquals("private, no-store", response.headers().firstValue("Cache-Control").orElse(null))
            response.body().use { lines -> assertEquals(":)", lines.findFirst().orElse(null)) }
        }

        verify(streamService).open(argThat { type == ConnectionType.EVENTSOURCE && account == token })
        verify(streamService).subscribeToChannel(any(), argThat { channelName == ChannelName.PUBLIC_LOCAL_MEDIA })
        verify(streamService, timeout(10_000)).close(any())
    }
}
