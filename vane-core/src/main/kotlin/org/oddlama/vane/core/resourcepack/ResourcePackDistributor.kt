package org.oddlama.vane.core.resourcepack

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent
import io.papermc.paper.connection.PlayerConfigurationConnection
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent
import io.papermc.paper.event.connection.configuration.PlayerConnectionReconfigureEvent
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest
import net.kyori.adventure.resource.ResourcePackStatus
import net.kyori.adventure.text.Component
import org.bukkit.event.EventHandler
import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleGroup
import org.oddlama.vane.util.Nms.serverHandle
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.logging.Level

class ResourcePackDistributor(context: Context<Core?>) :
    Listener<Core?>(context.group("ResourcePack", "Enable resource pack distribution.")) {

    @ConfigBoolean(def = true, desc = "Kick players if they deny to use the specified resource pack (if set).")
    var configForce: Boolean = false

    @LangMessage
    var langPackRequired: TranslatedMessage? = null

    @LangMessage
    var langPackSuggested: TranslatedMessage? = null

    var packUrl: String? = null
    var packSha1: String? = null
    var packUuid: UUID = UUID.fromString("fbba121a-8f87-4e97-922d-2059777311bf")
    @JvmField
    var counter: Int = 0

    var customResourcePackConfig: CustomResourcePackConfig = CustomResourcePackConfig(requireNotNull(getContext()))
    private var fileWatcher: ResourcePackFileWatcher? = null
    private var devServer: ResourcePackDevServer? = null

    private val latches = ConcurrentHashMap<UUID, CountDownLatch>()

    public override fun onEnable() {
        val mod = requireNotNull(module)
        when {
            localDev -> {
                try {
                    val packOutput: File? = File("VaneResourcePack.zip").takeIf { it.exists() }
                        ?: mod.generateResourcePack().also {
                            if (it == null) {
                                mod.log.severe("Failed to generate resource pack; dev server and file watcher will not be started")
                            } else {
                                mod.log.info("Resource Pack Missing, first run? Generating resource pack.")
                            }
                        }

                    packOutput?.let { file ->
                        fileWatcher = ResourcePackFileWatcher(this, file)
                        devServer = ResourcePackDevServer(this, file).also { it.serve() }
                        fileWatcher?.watchForChanges()
                    }
                } catch (e: Exception) {
                    if (e is IOException || e is InterruptedException) {
                        mod.log.log(Level.SEVERE, "Failed to initialize resource pack dev server or file watcher", e)
                    } else throw e
                }
                mod.log.info("Setting up dev lazy server")
            }
            (customResourcePackConfig.getContext() as? ModuleGroup<*>)?.configEnabled == true -> {
                mod.log.info("Serving custom resource pack")
                packUrl = customResourcePackConfig.configUrl
                packSha1 = customResourcePackConfig.configSha1
                packUuid = UUID.fromString(customResourcePackConfig.configUuid)
            }
            else -> {
                mod.log.info("Serving official vane resource pack")
                try {
                    val properties = Properties().also { props ->
                        Core::class.java.getResourceAsStream("/vane-core.properties")!!.use { props.load(it) }
                    }
                    packUrl = properties.getProperty("resourcePackUrl")
                    packSha1 = properties.getProperty("resourcePackSha1")
                    packUuid = UUID.fromString(properties.getProperty("resourcePackUuid"))
                } catch (_: IOException) {
                    mod.log.severe("Could not load official resource pack sha1 from included properties file")
                    packUrl = ""
                    packSha1 = ""
                    packUuid = UUID.randomUUID()
                }
            }
        }

        val sha1 = requireNotNull(packSha1)
        // Check sha1 sum validity
        if (sha1.length != 40) {
            mod.log.warning("Invalid resource pack SHA-1 sum '$sha1', should be 40 characters long but has ${sha1.length} characters")
            mod.log.warning("Disabling resource pack serving and message delaying")
            packUrl = ""
        }

        // Propagate enable after determining whether the player message delayer is active
        super.onEnable()

        packSha1 = sha1.lowercase()
        if (packUrl!!.isNotEmpty()) {
            serverHandle().settings.properties.serverResourcePackInfo.ifPresent { rpInfo ->
                if (rpInfo.url().trim().isNotEmpty()) {
                    mod.log.warning(
                        "You have manually configured a resource pack in your server.properties. " +
                        "This cannot be used together with vane, as servers only allow serving a single resource pack."
                    )
                }
            }
            mod.log.info("Distributing resource pack from '$packUrl' with sha1 $packSha1")
        }
    }

    public override fun onDisable() {
        latches.values.forEach { it.countDown() }
        latches.clear()

        fileWatcher?.stop()
        fileWatcher = null
        devServer?.stop()
        devServer = null

        super.onDisable()
    }

    @EventHandler
    fun onPlayerAsyncConnectionConfigure(event: AsyncPlayerConnectionConfigureEvent) {
        val profileUuid = event.connection.profile.id ?: return

        val latch = CountDownLatch(1)
        latches.put(profileUuid, latch)?.countDown()

        sendResourcePackDuringConfiguration(event.connection)

        try {
            latch.await()
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            module?.log?.warning("Resource pack wait interrupted for player $profileUuid")
        }

        event.connection.completeReconfiguration()
    }

    @EventHandler
    fun onPlayerConnectionReconfigure(event: PlayerConnectionReconfigureEvent) {
        sendResourcePackDuringConfiguration(event.connection)
    }

    @EventHandler
    fun onPlayerConnectionClose(event: PlayerConnectionCloseEvent) {
        latches.remove(event.playerUniqueId)?.countDown()
    }

    fun sendResourcePackDuringConfiguration(connection: PlayerConfigurationConnection) {
        val info = ResourcePackInfo.resourcePackInfo(packUuid, URI.create(requireNotNull(packUrl)), requireNotNull(packSha1))
        val promptLang = requireNotNull(if (configForce) langPackRequired else langPackSuggested)
        val prompt: Component? = if (promptLang.str().isEmpty()) null else promptLang.strComponent()
        val request = ResourcePackRequest.resourcePackRequest()
            .required(configForce).replace(true)
            .packs(info)
            .callback { _: UUID?, status: ResourcePackStatus?, _: Audience? ->
                if (status?.intermediate() == false) {
                    latches.remove(connection.profile.id)?.countDown()
                }
            }
            .prompt(prompt)
            .build()

        connection.audience.sendResourcePacks(request)
    }

    fun sendResourcePack(audience: Audience) {
        var url = packUrl
        if (localDev) {
            url = "$packUrl?$counter"
            audience.sendMessage(Component.text("$url $packSha1"))
        }
        try {
            val info = ResourcePackInfo.resourcePackInfo(packUuid, URI.create(requireNotNull(url)), requireNotNull(packSha1))
            audience.sendResourcePacks(ResourcePackRequest.resourcePackRequest().packs(info).asResourcePackRequest())
        } catch (_: URISyntaxException) {
            module?.log?.warning("The provided resource pack URL is incorrect: $url")
        }
    }

    fun updateSha1(file: File) {
        if (!localDev) return
        try {
            val hashBytes = MessageDigest.getInstance("SHA-1").digest(file.readBytes())
            packSha1 = hashBytes.joinToString("") { "%02x".format(it) }
        } catch (_: IOException) {}
    }

    companion object {
        // Assume debug environment if both add-plugin and vane-debug are defined, until run-paper adds
        // a better way. https://github.com/jpenilla/run-paper/issues/14
        private val localDev =
            serverHandle().options.hasArgument("add-plugin") && System.getProperty("disable.watchdog") != null
    }
}
