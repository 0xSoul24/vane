package org.oddlama.vane.busybar

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import com.sun.net.httpserver.HttpsConfigurator
import com.sun.net.httpserver.HttpsServer
import org.json.JSONException
import org.bukkit.permissions.Permission
import org.json.JSONObject
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleComponent
import java.io.IOException
import java.net.InetSocketAddress
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.logging.Level

/**
 * HTTP server that bridges connect to, serving HTTPS unless [Tls] is off.
 *
 * - `GET /events` keeps a server-sent-events stream open. The first event is always `snapshot`.
 * - `POST /action` accepts a JSON request such as `{"type":"presence.set","busy":true}`.
 * - `GET /icon` returns the server icon as PNG, with an ETag the snapshot also carries.
 * - `GET /head/<id>` returns a player's 8x8 face as PNG, for the skin id in join and quit events.
 *
 * Both endpoints require `Authorization: Bearer <token>` with a token from `/busybar link`. What a
 * bridge receives and may request is limited by its owner's permissions, see [BridgeAccess].
 * Every connection runs on its own virtual thread; events are handed over through a bounded queue
 * per connection, so the main thread never blocks on a slow bridge.
 *
 * @param context owning context.
 */
class EventStream(context: Context<BusyBar?>) : ModuleComponent<BusyBar?>(context) {
    private val busybar: BusyBar
        get() = requireNotNull(module)

    /** One connected bridge. */
    private class Client(val player: UUID) {
        /** Encoded frames waiting to be written, ended by [CLOSE]. One extra slot keeps room for it. */
        val queue = LinkedBlockingQueue<String>(QUEUE_CAPACITY + 1)
    }

    /** Running HTTP server, if any. */
    private var httpServer: HttpServer? = null

    /** Executor running the connection handlers. */
    private var executor: ExecutorService? = null

    /** Connected bridges. */
    private val clients = CopyOnWriteArrayList<Client>()

    /** Whether `server.stop` was already sent during this shutdown. */
    private var stopAnnounced = false

    /** Monotonic SSE event id. */
    private val nextId = AtomicLong()

    /** The server icon as served by `/icon`. */
    private class Icon(val png: ByteArray, val etag: String)

    /** Server icon read at enabling, or null when the server has none. */
    @Volatile
    private var icon: Icon? = null

    /** ETag of the server icon, or null when the server has none. Bridges compare it to their cached copy. */
    fun iconTag(): String? = icon?.etag

    /** Starts the server. */
    override fun onEnable() {
        stopAnnounced = false
        icon = loadIcon()
        startServer()
    }

    /** Tells connected bridges the server is going away, then stops the server. */
    override fun onDisable() {
        announceStop()
        stopServer()
    }

    /** Restarts the server with freshly loaded TLS settings, disconnecting every bridge. Call from the main thread. */
    fun restart() {
        stopServer()
        startServer()
    }

    /** Binds the server, with TLS unless it is off. Logs and stays stopped on failure. */
    private fun startServer() {
        val exec = Executors.newVirtualThreadPerTaskExecutor()
        val address = InetSocketAddress(busybar.configBindAddress, busybar.configPort)
        try {
            val ssl = busybar.tls.load()
            val server = if (ssl == null) {
                HttpServer.create(address, 0)
            } else {
                HttpsServer.create(address, 0).apply { httpsConfigurator = HttpsConfigurator(ssl) }
            }
            httpServer = server.apply {
                createContext("/events", ::handleEvents)
                createContext("/action", ::handleAction)
                createContext("/icon", ::handleIcon)
                createContext("/head/", ::handleHead)
                executor = exec
                start()
            }
            executor = exec
            val protocol = if (ssl == null) "HTTP" else "HTTPS"
            busybar.log.info("BUSY Bar event stream listening on $address ($protocol, Tls ${busybar.tls.mode.name.lowercase()})")
        } catch (e: IOException) {
            exec.shutdownNow()
            busybar.log.log(Level.SEVERE, "Failed to start the BUSY Bar event stream on $address", e)
        } catch (e: GeneralSecurityException) {
            exec.shutdownNow()
            busybar.log.log(
                Level.SEVERE,
                "Failed to set up TLS for the BUSY Bar event stream. With Tls auto, delete tls-auto.key and " +
                    "tls-auto.crt from the plugin folder to generate a new certificate; with Tls keystore, check " +
                    "TlsKeystore and TlsKeystorePassword.",
                e
            )
        }
    }

    /** Closes every bridge connection and stops the server. */
    private fun stopServer() {
        clients.forEach(::close)
        httpServer?.stop(1)
        httpServer = null
        executor?.shutdownNow()
        executor = null
    }

    /**
     * Queues an event for connected bridges. Call from the main thread.
     *
     * @param type event name, for example `player.join`.
     * @param data event payload.
     * @param target only deliver to this player's bridges, or to everyone when null.
     * @param permission only deliver to bridges whose owner holds this permission, or to everyone when null.
     */
    fun emit(type: String, data: JSONObject, target: UUID? = null, permission: Permission? = null) {
        if (clients.isEmpty()) return
        val frame = frame(type, data)
        for (client in clients) {
            if (target != null && client.player != target) continue
            if (permission != null && !busybar.access.has(client.player, permission)) continue
            if (client.queue.size >= QUEUE_CAPACITY || !client.queue.offer(frame)) {
                busybar.log.warning("Dropping BUSY Bar bridge of $target: it is not keeping up with events.")
                close(client, discardPending = true)
            }
        }
    }

    /** Sends `server.stop` once per shutdown, from whichever comes first: the shutdown kicks or disabling. */
    fun announceStop() {
        if (stopAnnounced) return
        stopAnnounced = true
        emit("server.stop", JSONObject().put("reason", "shutdown"))
    }

    /** Closes every bridge connection of [player]. */
    fun disconnect(player: UUID) = clients.filter { it.player == player }.forEach(::close)

    /** Number of bridges currently connected for [player]. */
    fun connectedCount(player: UUID): Int = clients.count { it.player == player }

    /**
     * Asks the handler of [client] to finish once it has written what is queued. A bridge that
     * fell behind ([discardPending]) loses its backlog instead, so the close marker fits.
     */
    private fun close(client: Client, discardPending: Boolean = false) {
        if (discardPending) client.queue.clear()
        if (!client.queue.offer(CLOSE)) {
            client.queue.clear()
            client.queue.offer(CLOSE)
        }
    }

    /** Encodes one server-sent event. */
    private fun frame(type: String, data: JSONObject): String =
        "id: ${nextId.incrementAndGet()}\nevent: $type\ndata: $data\n\n"

    /** Returns the player for the request's bearer token or null. */
    private fun authenticate(exchange: HttpExchange): UUID? {
        val header = exchange.requestHeaders.getFirst("Authorization") ?: return null
        if (!header.startsWith("Bearer ")) return null
        return busybar.playerForToken(header.removePrefix("Bearer ").trim())
    }

    /** Serves `GET /events`. */
    private fun handleEvents(exchange: HttpExchange) {
        try {
            if (exchange.requestMethod != "GET") return exchange.sendResponseHeaders(405, -1)
            val player = authenticate(exchange) ?: return exchange.sendResponseHeaders(401, -1)

            val snapshot = try {
                busybar.server.scheduler.callSyncMethod(busybar) { busybar.snapshot(player) }
                    .get(SYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            } catch (_: Exception) {
                return exchange.sendResponseHeaders(503, -1)
            }

            exchange.responseHeaders.apply {
                add("Content-Type", "text/event-stream; charset=utf-8")
                add("Cache-Control", "no-cache")
            }
            exchange.sendResponseHeaders(200, 0)

            val client = Client(player)
            clients += client
            try {
                val out = exchange.responseBody
                out.write(frame("snapshot", snapshot).toByteArray())
                out.flush()
                // Runs until the close marker, so frames queued before it (such as server.stop) are sent.
                while (true) {
                    val next = client.queue.poll(HEARTBEAT_SECONDS, TimeUnit.SECONDS)
                    if (next === CLOSE) break
                    out.write((next ?: HEARTBEAT).toByteArray())
                    out.flush()
                }
            } finally {
                clients -= client
            }
        } catch (_: IOException) {
            // Bridge went away.
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        } finally {
            exchange.close()
        }
    }

    /** Serves `POST /action`. */
    private fun handleAction(exchange: HttpExchange) {
        try {
            if (exchange.requestMethod != "POST") return exchange.sendResponseHeaders(405, -1)
            val player = authenticate(exchange) ?: return exchange.sendResponseHeaders(401, -1)

            val body = exchange.requestBody.readNBytes(MAX_REQUEST_BYTES + 1)
            if (body.size > MAX_REQUEST_BYTES) return exchange.sendResponseHeaders(413, -1)
            val request = try {
                JSONObject(String(body))
            } catch (_: JSONException) {
                return exchange.sendResponseHeaders(400, -1)
            }

            val action = when (request.optString("type")) {
                "presence.set" -> "presence.set"
                "action.invoke" -> request.optString("action")
                else -> return exchange.sendResponseHeaders(400, -1)
            }
            val permission = busybar.permissionForAction(action) ?: return exchange.sendResponseHeaders(400, -1)
            if (!busybar.actionAllowed(action) || !busybar.access.has(player, permission)) {
                return exchange.sendResponseHeaders(403, -1)
            }

            busybar.scheduleNextTick { busybar.handleRequest(player, request) }
            exchange.sendResponseHeaders(202, -1)
        } catch (_: IOException) {
            // Bridge went away.
        } finally {
            exchange.close()
        }
    }

    /** Serves `GET /icon`. */
    private fun handleIcon(exchange: HttpExchange) {
        try {
            if (exchange.requestMethod != "GET") return exchange.sendResponseHeaders(405, -1)
            if (authenticate(exchange) == null) return exchange.sendResponseHeaders(401, -1)
            val current = icon ?: return exchange.sendResponseHeaders(404, -1)

            exchange.responseHeaders.add("ETag", current.etag)
            if (exchange.requestHeaders.getFirst("If-None-Match") == current.etag) {
                return exchange.sendResponseHeaders(304, -1)
            }
            exchange.responseHeaders.add("Content-Type", "image/png")
            exchange.sendResponseHeaders(200, current.png.size.toLong())
            exchange.responseBody.write(current.png)
        } catch (_: IOException) {
            // Bridge went away.
        } finally {
            exchange.close()
        }
    }

    /** Serves `GET /head/<id>`. Downloads the skin on first use, on this handler's own thread. */
    private fun handleHead(exchange: HttpExchange) {
        try {
            if (exchange.requestMethod != "GET") return exchange.sendResponseHeaders(405, -1)
            if (authenticate(exchange) == null) return exchange.sendResponseHeaders(401, -1)
            val id = exchange.requestURI.path.removePrefix("/head/")
            val png = try {
                busybar.heads.face(id)
            } catch (e: IOException) {
                busybar.log.warning("Could not render the face for skin $id: ${e.message}")
                return exchange.sendResponseHeaders(502, -1)
            } ?: return exchange.sendResponseHeaders(404, -1)

            exchange.responseHeaders.apply {
                add("Content-Type", "image/png")
                // A skin id names one texture forever, so the face never changes.
                add("Cache-Control", "max-age=31536000, immutable")
            }
            exchange.sendResponseHeaders(200, png.size.toLong())
            exchange.responseBody.write(png)
        } catch (_: IOException) {
            // Bridge went away.
        } finally {
            exchange.close()
        }
    }

    /** Decodes the server icon Paper loaded from `server-icon.png`. Call from the main thread. */
    private fun loadIcon(): Icon? {
        val data = busybar.server.serverIcon?.data ?: return null
        val png = try {
            Base64.getDecoder().decode(data.substringAfter("base64,"))
        } catch (_: IllegalArgumentException) {
            return null
        }
        if (png.isEmpty()) return null
        val hash = MessageDigest.getInstance("SHA-256").digest(png).take(ETAG_BYTES).joinToString("") { "%02x".format(it) }
        return Icon(png, "\"$hash\"")
    }

    companion object {
        /** Hash bytes used for the icon ETag. */
        private const val ETAG_BYTES = 8

        /** Frames buffered per bridge before it is considered stuck. Bursts such as a proxy restart are
         * emptying a large server queue one frame per player, so leave plenty of room. */
        private const val QUEUE_CAPACITY = 1024

        /** Seconds between keep-alive comments on an idle stream. */
        private const val HEARTBEAT_SECONDS = 15L

        /** Seconds to wait for the main thread to build a snapshot. */
        private const val SYNC_TIMEOUT_SECONDS = 5L

        /** Largest accepted request body. */
        private const val MAX_REQUEST_BYTES = 4096

        /** SSE comment sent when nothing happened for a while. */
        private const val HEARTBEAT = ": ping\n\n"

        /** Sentinel telling a handler to finish; compared by identity. */
        private val CLOSE = String(CharArray(0))
    }
}
