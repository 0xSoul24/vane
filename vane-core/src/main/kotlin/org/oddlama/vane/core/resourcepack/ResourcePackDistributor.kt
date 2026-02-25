package org.oddlama.vane.core.resourcepack

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent
import com.google.common.io.Files
import java.security.MessageDigest
import io.papermc.paper.connection.PlayerConfigurationConnection
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent
import io.papermc.paper.event.connection.configuration.PlayerConnectionReconfigureEvent
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest
import net.kyori.adventure.resource.ResourcePackStatus
import net.kyori.adventure.text.Component
import net.minecraft.server.MinecraftServer
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
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.function.Consumer
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

    var customResourcePackConfig: CustomResourcePackConfig = CustomResourcePackConfig(getContext()!!)
    private var fileWatcher: ResourcePackFileWatcher? = null
    private var devServer: ResourcePackDevServer? = null

    private val latches = ConcurrentHashMap<UUID, CountDownLatch>()

    public override fun onEnable() {
        if (localDev) {
            try {
                var packOutput: File? = File("VaneResourcePack.zip")
                if (!packOutput!!.exists()) {
                    module!!.log.info("Resource Pack Missing, first run? Generating resource pack.")
                    packOutput = module!!.generateResourcePack()
                }

                if (packOutput != null) {
                    fileWatcher = ResourcePackFileWatcher(this, packOutput)
                    devServer = ResourcePackDevServer(this, packOutput)
                    devServer!!.serve()
                    fileWatcher!!.watchForChanges()
                } else {
                    module!!.log.severe("Failed to generate resource pack; dev server and file watcher will not be started")
                }
            } catch (e: IOException) {
                module!!.log.log(
                    Level.SEVERE,
                    "Failed to initialize resource pack dev server or file watcher",
                    e
                )
            } catch (e: InterruptedException) {
                module!!.log.log(
                    Level.SEVERE,
                    "Failed to initialize resource pack dev server or file watcher",
                    e
                )
            }

            module!!.log.info("Setting up dev lazy server")
        } else if ((customResourcePackConfig.getContext() as? ModuleGroup<*>)?.configEnabled == true) {
            module!!.log.info("Serving custom resource pack")
            packUrl = customResourcePackConfig.configUrl
            packSha1 = customResourcePackConfig.configSha1
            packUuid = UUID.fromString(customResourcePackConfig.configUuid)
        } else {
            val group = customResourcePackConfig.getContext() as? ModuleGroup<*>
            if (group?.configEnabled == true) {
                module!!.log.info("Serving custom resource pack")
                packUrl = customResourcePackConfig.configUrl
                packSha1 = customResourcePackConfig.configSha1
                packUuid = UUID.fromString(customResourcePackConfig.configUuid)
            } else {
                module!!.log.info("Serving official vane resource pack")
                try {
                    val properties = Properties()
                    properties.load(Core::class.java.getResourceAsStream("/vane-core.properties"))
                    packUrl = properties.getProperty("resourcePackUrl")
                    packSha1 = properties.getProperty("resourcePackSha1")
                    packUuid = UUID.fromString(properties.getProperty("resourcePackUuid"))
                } catch (e: IOException) {
                    module!!.log.severe("Could not load official resource pack sha1 from included properties file")
                    packUrl = ""
                    packSha1 = ""
                    packUuid = UUID.randomUUID()
                }
            }
        }

        // Check sha1 sum validity
        if (packSha1!!.length != 40) {
            module!!
                .log.warning(
                    "Invalid resource pack SHA-1 sum '" +
                            packSha1 +
                            "', should be 40 characters long but has " +
                            packSha1!!.length +
                            " characters"
                )
            module!!.log.warning("Disabling resource pack serving and message delaying")

            // Disable resource pack
            packUrl = ""
        }

        // Propagate enable after determining whether the player message delayer is active,
        // so it is only enabled when needed.
        super.onEnable()

        packSha1 = packSha1!!.lowercase(Locale.getDefault())
        if (!packUrl!!.isEmpty()) {
            // Check if the server has a manually configured resource pack.
            // This would conflict.
            serverHandle()!!
                .settings.properties
                .serverResourcePackInfo.ifPresent(Consumer { rpInfo: MinecraftServer.ServerResourcePackInfo? ->
                    if (!rpInfo!!.url().trim { it <= ' ' }.isEmpty()) {
                        module!!
                            .log.warning(
                                "You have manually configured a resource pack in your server.properties. This cannot be used together with vane, as servers only allow serving a single resource pack."
                            )
                    }
                })

            module!!.log.info("Distributing resource pack from '$packUrl' with sha1 $packSha1")
        }
    }

    public override fun onDisable() {
        // Release all pending configuration latches so blocked threads can exit
        for (latch in latches.values) {
            latch.countDown()
        }
        latches.clear()

        if (fileWatcher != null) {
            fileWatcher!!.stop()
            fileWatcher = null
        }
        if (devServer != null) {
            devServer!!.stop()
            devServer = null
        }

        super.onDisable()
    }

    @EventHandler
    fun onPlayerAsyncConnectionConfigure(event: AsyncPlayerConnectionConfigureEvent) {
        val profileUuid = event.connection.profile.id ?: return

        // Block the thread to prevent the question screen from going away
        val latch = CountDownLatch(1)
        val oldLatch = latches.put(profileUuid, latch)
        oldLatch?.countDown()

        sendResourcePackDuringConfiguration(event.connection)

        try {
            latch.await()
        } catch (ex: InterruptedException) {
            Thread.currentThread().interrupt()
            module!!.log.warning("Resource pack wait interrupted for player $profileUuid")
        }

        event.connection.completeReconfiguration()
    }

    @EventHandler
    fun onPlayerConnectionReconfigure(event: PlayerConnectionReconfigureEvent) {
        sendResourcePackDuringConfiguration(event.connection)
    }

    @EventHandler
    fun onPlayerConnectionClose(event: PlayerConnectionCloseEvent) {
        // Cleanup
        Optional.ofNullable<CountDownLatch?>(latches.remove(event.playerUniqueId))
            .ifPresent(Consumer { obj: CountDownLatch? -> obj!!.countDown() })
    }

    fun sendResourcePackDuringConfiguration(connection: PlayerConfigurationConnection) {
        val info = ResourcePackInfo.resourcePackInfo(packUuid, URI.create(packUrl!!), packSha1!!)
        val promptLang = (if (configForce) langPackRequired else langPackSuggested)!!
        val prompt = if (promptLang.str().isEmpty()) null else promptLang.strComponent()
        val request = ResourcePackRequest.resourcePackRequest()
            .required(configForce).replace(true)
            .packs(info)
            .callback { uuid: UUID?, status: ResourcePackStatus?, audience: Audience? ->
                if (!status!!.intermediate()) {
                    Optional.ofNullable<CountDownLatch?>(latches.remove(connection.profile.id))
                        .ifPresent(Consumer { obj: CountDownLatch? -> obj!!.countDown() })
                }
            }
            .prompt(prompt)
            .build()

        connection.audience.sendResourcePacks(request)
    }

    // For sending the resource pack during gameplay
    fun sendResourcePack(audience: Audience) {
        var url2 = packUrl
        if (localDev) {
            url2 = "$packUrl?$counter"
            audience.sendMessage(Component.text("$url2 $packSha1"))
        }

        try {
            val info = ResourcePackInfo.resourcePackInfo(packUuid, URI(url2!!), packSha1!!)
            audience.sendResourcePacks(ResourcePackRequest.resourcePackRequest().packs(info).asResourcePackRequest())
        } catch (e: URISyntaxException) {
            module!!.log.warning("The provided resource pack URL is incorrect: $url2")
        }
    }

    fun updateSha1(file: File) {
        if (!localDev) return
        try {
            val digest = MessageDigest.getInstance("SHA-1")
            val hashBytes = digest.digest(Files.asByteSource(file).read())
            this@ResourcePackDistributor.packSha1 = hashBytes.joinToString("") { "%02x".format(it) }
        } catch (ignored: IOException) {
        }
    }

    companion object {
        // Assume debug environment if both add-plugin and vane-debug are defined, until run-paper adds
        // a better way.
        // https://github.com/jpenilla/run-paper/issues/14
        private val localDev =
            serverHandle()!!.options.hasArgument("add-plugin") && java.lang.Boolean.getBoolean("disable.watchdog")
    }
}
