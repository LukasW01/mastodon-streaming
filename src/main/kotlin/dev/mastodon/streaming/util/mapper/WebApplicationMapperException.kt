package dev.mastodon.streaming.util.mapper

import dev.mastodon.streaming.dto.ErrorResponse
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

open class WebApplicationMapperException(message: String, val status: Int) : WebApplicationException(
    message,
    Response.status(status).type(MediaType.APPLICATION_JSON).entity(ErrorResponse(message))
        .build(),
)

/**
 * Extracts the status and message properties from the error object, if
 * available for public use.
 */
fun Throwable.toErrorResponse(): ErrorResponse = when (this) {
    is WebApplicationMapperException -> ErrorResponse(message.orEmpty(), status)
    else -> ErrorResponse(Response.Status.INTERNAL_SERVER_ERROR.reasonPhrase, Response.Status.INTERNAL_SERVER_ERROR.statusCode)
}
