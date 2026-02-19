package dev.mastodon.streaming

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import jakarta.ws.rs.core.MediaType
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

@QuarkusTest
class InfoControllerTest {
    @Test
    fun testHealth() {
        given()
            .`when`()
            .get("/health")
            .then()
            .statusCode(200)
            .contentType(MediaType.APPLICATION_JSON)
            .body("status", equalTo(200))
            .body("text", equalTo("OK"))
    }
}
