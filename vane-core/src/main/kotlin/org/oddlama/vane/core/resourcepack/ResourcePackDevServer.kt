package org.oddlama.vane.core.resourcepack

import com.google.common.io.Files
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.net.InetSocketAddress
import java.security.MessageDigest
import java.util.logging.Level

class ResourcePackDevServer(private val resourcePackDistributor: ResourcePackDistributor, private val file: File) :
    HttpHandler {
    private var httpServer: HttpServer? = null

    fun stop() {
        if (httpServer != null) {
            httpServer!!.stop(0)
            httpServer = null
        }
    }

    fun serve() {
        try {
            httpServer = HttpServer.create(InetSocketAddress(9000), 0)
            val digest = MessageDigest.getInstance("SHA-1")
            val hashBytes = digest.digest(Files.asByteSource(this.file).read())
            resourcePackDistributor.packSha1 = hashBytes.joinToString("") { "%02x".format(it) }
            resourcePackDistributor.packUrl = "http://localhost:9000/vane-resource-pack.zip"

            this.httpServer!!.createContext("/", this)
            this.httpServer!!.executor = null
            this.httpServer!!.start()
        } catch (e: IOException) {
            resourcePackDistributor.module!!.log.log(
                Level.SEVERE,
                "Failed to start resource pack dev server",
                e
            )
        }
    }

    @Throws(IOException::class)
    override fun handle(he: HttpExchange) {
        val method = he.requestMethod
        if (!("HEAD" == method || "GET" == method)) {
            he.sendResponseHeaders(501, -1)
            return
        }

        val fis: FileInputStream?
        try {
            fis = FileInputStream(file)
        } catch (e: FileNotFoundException) {
            he.sendResponseHeaders(404, -1)
            return
        }

        he.responseHeaders.set("Content-Type", "application/zip")
        if ("GET" == method) {
            he.sendResponseHeaders(200, file.length())
            val os = he.responseBody
            fis.transferTo(os)
            os.close()
        } else {
            he.sendResponseHeaders(200, -1)
        }
        fis.close()
    }
}
