package dev.mastodon.streaming.controller

import dev.mastodon.streaming.dto.StreamParams
import dev.mastodon.streaming.service.ChannelService
import dev.mastodon.streaming.service.StreamService
import dev.mastodon.streaming.util.ChannelName
import dev.mastodon.streaming.util.mapper.WebApplicationMapperException
import dev.mastodon.streaming.util.primitives.isTruthy
import dev.mastodon.streaming.util.resolver.AccountResolver
import dev.mastodon.streaming.util.session.EventSourceSession

import io.smallrye.common.annotation.Blocking
import io.smallrye.mutiny.Multi
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerRequest
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.sse.OutboundSseEvent
import jakarta.ws.rs.sse.Sse
import org.jboss.resteasy.reactive.ResponseHeader
import org.jboss.resteasy.reactive.RestPath

import java.time.Duration

/** Streams to clients over eventsource, the path decides which stream is requested. */
@ApplicationScoped
@Path("/")
class EventSourceController {
    @Inject
    private lateinit var accountResolver: AccountResolver

    @Inject
    private lateinit var channelService: ChannelService

    @Inject
    private lateinit var streamService: StreamService

    @Inject
    private lateinit var vertx: Vertx
    
    @GET
    @Path("/api/v1/streaming/{path: .+}")
    @Blocking
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @ResponseHeader(name = "Cache-Control", value = ["private, no-store"])
    fun stream(
        @RestPath path: String,
        request: HttpServerRequest,
        @Context sse: Sse
    ): Multi<OutboundSseEvent> {
        // If no channelName can be found for the request, then we should terminate
        // the connection, as there's nothing to stream back
        val channelName = ChannelName.fromPath(path, request.getParam("only_media").isTruthy())
            ?: throw WebApplicationMapperException("Unknown channel requested", Response.Status.BAD_REQUEST.statusCode)

        val account = runCatching { accountResolver.accountFromRequest(request).checkScopes(channelName) }.getOrNull()
            ?: throw WebApplicationMapperException("Missing or invalid access token", Response.Status.UNAUTHORIZED.statusCode)

        val resolution = channelService.channelNameToIds(account, channelName, StreamParams(tag = request.getParam("tag"), list = request.getParam("list")))

        return Multi.createFrom().emitter { emitter ->
            EventSourceSession(emitter, sse, account).let { session ->
                session.sendComment(")")

                vertx.setPeriodic(Duration.ofSeconds(15L).toMillis()) { session.sendComment("thump") }.let { heartbeat ->
                    emitter.onTermination {
                        vertx.cancelTimer(heartbeat)
                        streamService.close(session)
                    }
                }
                
                streamService.open(session)
                streamService.subscribeToChannel(session, resolution)
            }
        }
    }
}
