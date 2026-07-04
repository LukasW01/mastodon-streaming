package dev.mastodon.streaming.util.mapper

import dev.mastodon.streaming.dto.ErrorResponse
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response

class WebApplicationMapperException(response: Response) : WebApplicationException(response) {
    constructor(message: String, status: Int) : this(
        Response.status(status).entity(ErrorResponse(message, status)).build(),
    )
}
