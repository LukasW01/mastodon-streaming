package dev.mastodon.streaming.controller

import dev.mastodon.streaming.dto.ErrorResponse
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@ApplicationScoped
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class InfoController {
    @GET
    @Path("/health")
    fun health(): Response = Response.status(Response.Status.OK).entity(ErrorResponse("Healthy!", Response.Status.OK.statusCode)).build()
}
