package dev.mastodon.streaming.controller

import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.jboss.resteasy.reactive.ResponseHeader

@ApplicationScoped
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class InfoController {
    @GET
    @Path("/health")
    @Produces(MediaType.TEXT_PLAIN)
    @ResponseHeader(name = "Cache-Control", value = ["private, no-store"])
    fun health(): String = "OK"
}
