package dev.mastodon.streaming.controller

import dev.mastodon.streaming.dto.ErrorResponse
import dev.mastodon.streaming.dto.StreamParams
import dev.mastodon.streaming.dto.Subscribe
import dev.mastodon.streaming.service.ChannelService
import dev.mastodon.streaming.service.StreamService
import dev.mastodon.streaming.util.ChannelName
import dev.mastodon.streaming.util.session.WsSession
import dev.mastodon.streaming.util.resolver.WebSocketHttpUpgradeAuth

import io.netty.handler.codec.http.QueryStringDecoder
import io.quarkus.logging.Log
import io.quarkus.websockets.next.OnClose
import io.quarkus.websockets.next.OnError
import io.quarkus.websockets.next.OnOpen
import io.quarkus.websockets.next.OnTextMessage
import io.quarkus.websockets.next.TextDecodeException
import io.quarkus.websockets.next.UserData
import io.quarkus.websockets.next.WebSocket
import io.quarkus.websockets.next.WebSocketConnection
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

/**
 * Streams to clients over a websocket. Clients are authenticated by
 * [WebSocketHttpUpgradeAuth] and can subscribe to multiple streams.
 */
@ApplicationScoped
@WebSocket(path = "/api/v1/streaming")
class WebSocketController {
    @Inject
    private lateinit var channelService: ChannelService

    @Inject
    private lateinit var streamService: StreamService

    @OnOpen
    fun onOpen(ws: WebSocketConnection) {
        WsSession(ws, ws.userData().get(UserData.TypedKey("account"))).let { session ->
            ws.userData().put(UserData.TypedKey("session"), session)

            streamService.open(session)

            // Parse the URL for the connection arguments (if supplied)
            val query = QueryStringDecoder("?${ws.handshakeRequest().query().orEmpty()}").parameters()
            val stream = query["stream"]?.firstOrNull()
            if (!stream.isNullOrEmpty()) {
                val params = StreamParams(tag = query["tag"]?.firstOrNull(), list = query["list"]?.firstOrNull())
                subscribeToChannel(session, stream, params)
            }
        }
    }

    @OnClose
    fun onClose(ws: WebSocketConnection) {
        // Note: the error handling of websockets next closes the connection on
        // failures, so that this is the only place to clean up.
        streamService.close(ws.userData().get(UserData.TypedKey<WsSession>("session")))
    }

    @OnTextMessage
    fun onMessage(ws: WebSocketConnection, message: Subscribe) {
        val session = ws.userData().get(UserData.TypedKey<WsSession>("session"))
        val stream = message.stream?.firstOrNull()
        val params = StreamParams(tag = message.tag, list = message.list)

        when (message.type) {
            "subscribe" -> subscribeToChannel(session, stream, params)
            "unsubscribe" -> unsubscribeFromChannel(session, stream, params)
            // Unknown action type
            else -> Unit
        }
    }

    /**
     * Ignores messages that cannot be read as [Subscribe], as Mastodon does.
     * Unhandled, the failure would close the connection.
     */
    @OnError
    fun onDecodeError(ws: WebSocketConnection, err: TextDecodeException) {
        Log.info("Error parsing message from user ${ws.userData().get(UserData.TypedKey<WsSession>("session")).account.accountId}", err)
    }

    private fun subscribeToChannel(
        session: WsSession,
        stream: String?,
        params: StreamParams
    ) {
        try {
            ChannelName.fromValue(stream).let { channelName ->
                session.account.checkScopes(channelName)

                streamService.subscribeToChannel(session, channelService.channelNameToIds(session.account, channelName, params))
            }
        } catch (err: RuntimeException) {
            Log.error("Websocket subscribe error", err)

            session.sendCustom(ErrorResponse("Error subscribing from channel"))
        }
    }

    private fun unsubscribeFromChannel(
        session: WsSession,
        stream: String?,
        params: StreamParams
    ) {
        try {
            channelService.channelNameToIds(session.account, ChannelName.fromValue(stream), params).let { resolution ->
                streamService.removeSubscription(session, resolution.channelIds)
            }
        } catch (err: RuntimeException) {
            Log.error("Websocket unsubscribe error", err)

            session.sendCustom(ErrorResponse("Error unsubscribing from channel"))
        }
    }

}
