package org.oddlama.vane.proxycore.config

import com.electronwill.nightconfig.core.CommentedConfig
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO

@Suppress("unused")
class ManagedServer(
    private val id: String,
    displayName: String,
    onlineConfigSection: CommentedConfig?,
    offlineConfigSection: CommentedConfig?,
    start: CommentedConfig
) {
    @JvmField
    var start: ServerStart = ServerStart(id, displayName, start)

    private val onlineConfig: StatefulConfiguration = StatefulConfiguration(id, displayName, onlineConfigSection)
    private val offlineConfig: StatefulConfiguration = StatefulConfiguration(id, displayName, offlineConfigSection)

    fun id(): String {
        return this.id
    }

    fun favicon(source: ConfigItemSource): String? {
        return when (source) {
            ConfigItemSource.ONLINE -> {
                onlineConfig.encodedFavicon
            }

            ConfigItemSource.OFFLINE -> {
                offlineConfig.encodedFavicon
            }

        }
    }

    fun startCmd(): Array<String?>? {
        return start.cmd
    }

    fun startKickMsg(): String? {
        return start.kickMsg
    }

    private fun randomQuote(source: ConfigItemSource): String? {
        val quoteSet = when (source) {
            ConfigItemSource.ONLINE -> onlineConfig.quotes
            ConfigItemSource.OFFLINE -> offlineConfig.quotes
        }

        if (quoteSet.isNullOrEmpty()) {
            return ""
        }
        return quoteSet[Random().nextInt(quoteSet.size)]
    }

    fun motd(source: ConfigItemSource): String {
        val sourcedMotd: String?
        val quoteSource: ConfigItemSource
        when (source) {
            ConfigItemSource.ONLINE -> {
                sourcedMotd = onlineConfig.motd
                quoteSource = ConfigItemSource.ONLINE
            }

            ConfigItemSource.OFFLINE -> {
                sourcedMotd = offlineConfig.motd
                quoteSource = ConfigItemSource.OFFLINE
            }

        }

        if (sourcedMotd == null) {
            return ""
        }
        return sourcedMotd.replace("{QUOTE}", randomQuote(quoteSource)!!)
    }

    fun commandTimeout(): Int? {
        return start.timeout
    }

    enum class ConfigItemSource {
        ONLINE,
        OFFLINE,
    }

    private class StatefulConfiguration(id: String, displayName: String, config: CommentedConfig?) {
        var quotes: Array<String?>? = null
        var motd: String? = null
        var encodedFavicon: String? = null

        init {
            // [managedServers.my_server.state]
            if (config != null) {
                // quotes = ["", ...]
                val quotesList = config.get<MutableList<String?>?>("quotes")

                if (quotesList != null) {
                    this.quotes = quotesList
                        .filter { s -> !s.isNullOrBlank() }
                        .map { s -> s!!.replace("{SERVER}", id).replace("{SERVER_DISPLAY_NAME}", displayName) }
                        .toTypedArray()
                }

                // motd = "..."
                val motdVal = config.get<Any?>("motd")

                require(motdVal == null || motdVal is String) { "Managed server '$id' has a non-string MOTD!" }

                if (motdVal is String && motdVal.isNotEmpty()) this.motd = motdVal.replace(
                    "{SERVER_DISPLAY_NAME}",
                    displayName
                )

                // favicon = "..."
                val faviconPath = config.get<Any?>("favicon")

                require(faviconPath == null || faviconPath is String) { "Managed server '$id' has a non-string favicon path!" }

                if (faviconPath is String && faviconPath.isNotEmpty()) this.encodedFavicon = encodeFavicon(
                    id,
                    faviconPath
                )
            }
        }

        companion object {
            @Throws(IOException::class)
            private fun encodeFavicon(id: String, faviconPath: String): String {
                val faviconFile = File(faviconPath.replace("{SERVER}", id))
                val image: BufferedImage
                try {
                    image = ImageIO.read(faviconFile)
                } catch (e: IOException) {
                    throw IOException("Failed to read favicon file: " + e.message)
                }

                require(!(image.width != 64 || image.height != 64)) { "Favicon has invalid dimensions (must be 64x64)" }

                val stream = ByteArrayOutputStream()
                ImageIO.write(image, "PNG", stream)
                val faviconBytes = stream.toByteArray()

                val encodedFavicon = "data:image/png;base64," + Base64.getEncoder().encodeToString(faviconBytes)

                require(encodedFavicon.length <= Short.MAX_VALUE) { "Favicon file too large for server to process" }

                return encodedFavicon
            }
        }
    }

    class ServerStart(id: String, displayName: String, config: CommentedConfig) {
        var cmd: Array<String?>?
        var timeout: Int?
        var kickMsg: String? = null
        @JvmField
        var allowAnyone: Boolean = false

        init {
            val cmdList = config.get<MutableList<String?>?>("cmd")
            val timeoutVal = config.get<Any?>("timeout")
            val kickMsgVal = config.get<Any?>("kickMsg")
            val allowAnyoneVal = config.get<Any?>("allowAnyone")

            if (cmdList != null) this.cmd = cmdList.map { s -> s!!.replace("{SERVER}", id) }.toTypedArray()
            else this.cmd = null

            require(kickMsgVal == null || kickMsgVal is String) { "Managed server '$id' has an invalid kick message!" }

            if (kickMsgVal is String) this.kickMsg = kickMsgVal.replace("{SERVER}", id).replace(
                "{SERVER_DISPLAY_NAME}",
                displayName
            )
            else this.kickMsg = null

            if (allowAnyoneVal is Boolean) this.allowAnyone = allowAnyoneVal
            else this.allowAnyone = false

            if (timeoutVal == null) {
                this.timeout = DEFAULT_TIMEOUT_SECONDS
            } else {
                require(timeoutVal is Int) { "Managed server '$id' has an invalid command timeout!" }

                this.timeout = timeoutVal
            }
        }

        companion object {
            private const val DEFAULT_TIMEOUT_SECONDS = 10
        }
    }
}
