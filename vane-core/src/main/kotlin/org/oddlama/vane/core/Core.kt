package org.oddlama.vane.core

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.core.Holder
import net.minecraft.core.MappedRegistry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.json.JSONException
import org.oddlama.vane.annotation.VaneModule
import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.commands.CustomItem
import org.oddlama.vane.core.commands.Enchant
import org.oddlama.vane.core.commands.Vane
import org.oddlama.vane.core.enchantments.EnchantmentManager
import org.oddlama.vane.core.functional.Consumer1
import org.oddlama.vane.core.item.*
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.menu.MenuManager
import org.oddlama.vane.core.misc.AuthMultiplexer
import org.oddlama.vane.core.misc.CommandHider
import org.oddlama.vane.core.misc.HeadLibrary
import org.oddlama.vane.core.misc.LootChestProtector
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.core.module.ModuleComponent
import org.oddlama.vane.core.resourcepack.ResourcePackDistributor
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator
import org.oddlama.vane.util.IOUtil.readJsonFromUrl
import org.oddlama.vane.util.msToTicks
import java.io.File
import java.io.IOException
import java.net.URISyntaxException
import java.util.*
import java.util.logging.Level

@VaneModule(name = "core", bstats = 8637, configVersion = 6, langVersion = 5, storageVersion = 1)
class Core : Module<Core?>() {
    var enchantmentManager: EnchantmentManager?
    private val modelDataRegistry: CustomModelDataRegistry?
    private val itemRegistry: CustomItemRegistry?

    @ConfigBoolean(
        def = true,
        desc = "Allow loading of player heads in relevant menus. Disabling this will show all player heads using the Steve skin, which may perform better on low-performance servers and clients."
    )
    var configPlayerHeadsInMenus: Boolean = false

    @LangMessage var langCommandNotAPlayer: TranslatedMessage? = null
    @LangMessage var langCommandPermissionDenied: TranslatedMessage? = null
    @LangMessage var langInvalidTimeFormat: TranslatedMessage? = null

    // Module registry
    private val vaneModules: SortedSet<Module<*>> = TreeSet(compareBy { it.annotationName })

    val resourcePackDistributor: ResourcePackDistributor

    fun registerModule(module: Module<*>) {
        vaneModules.add(module)
    }

    fun unregisterModule(module: Module<*>) {
        vaneModules.remove(module)
    }

    val modules: SortedSet<Module<*>?>
        get() = Collections.unmodifiableSortedSet(vaneModules)

    // Vane global command catch-all permission
    var permissionCommandCatchall: Permission = Permission(
        "vane.*.commands.*",
        "Allow access to all vane commands (ONLY FOR ADMINS!)",
        PermissionDefault.FALSE
    )

    @JvmField
    var menuManager: MenuManager?

    // core-config
    @ConfigBoolean(
        def = true,
        desc = "Let the client translate messages using the generated resource pack. This allows every player to select their preferred language, and all plugin messages will also be translated. Disabling this won't allow you to skip generating the resource pack, as it will be needed for custom item textures."
    )
    var configClientSideTranslations: Boolean = false

    @ConfigBoolean(def = true, desc = "Send update notices to OPed player when a new version of vane is available.")
    var configUpdateNotices: Boolean = false

    var currentVersion: String? = null
    var latestVersion: String? = null

    init {
        check(INSTANCE == null) { "Cannot instanciate Core twice." }
        INSTANCE = this

        // Create global command catch-all permission
        registerPermission(permissionCommandCatchall)

        // Allow registration of new enchantments and entities
        unfreezeRegistries()

        // Components
        enchantmentManager = EnchantmentManager(this)
        HeadLibrary(this)
        AuthMultiplexer(this)
        LootChestProtector(this)
        VanillaFunctionalityInhibitor(this)
        DurabilityManager(this)
        Vane(this)
        CustomItem(this)
        Enchant(this)
        menuManager = MenuManager(this)
        resourcePackDistributor = ResourcePackDistributor(this)
        CommandHider(this)
        modelDataRegistry = CustomModelDataRegistry()
        itemRegistry = CustomItemRegistry()
        ExistingItemConverter(this)
    }

    override fun onModuleEnable() {
        if (configUpdateNotices) {
            // Now, and every hour after that, check if a new version is available.
            // OPs will get a message about this when they join.
            scheduleTaskTimer(::checkForUpdate, 1L, msToTicks(2 * 60L * 60L * 1000L))
        }
    }

    fun unfreezeRegistries() {
        // NOTE: MAGIC VALUES! Introduced for 1.18.2 when registries were frozen. Sad, no workaround
        // at the time.
        try {
            // Make relevant fields accessible
            val frozen = MappedRegistry::class.java.getDeclaredField("frozen" /* frozen */)
                .also { it.isAccessible = true }
            val intrusiveHolderCache = MappedRegistry::class.java.getDeclaredField(
                "unregisteredIntrusiveHolders" /* unregisteredIntrusiveHolders (1.19.3+), intrusiveHolderCache (until 1.19.2) */
            ).also { it.isAccessible = true }

            // Unfreeze required registries
            frozen.set(BuiltInRegistries.ENTITY_TYPE, false)
            intrusiveHolderCache.set(
                BuiltInRegistries.ENTITY_TYPE,
                IdentityHashMap<EntityType<*>?, Holder.Reference<EntityType<*>?>?>()
            )
            // Since 1.20.2 this is also needed for enchantments:
        } catch (e: Exception) {
            when (e) {
                is NoSuchFieldException, is SecurityException, is IllegalArgumentException, is IllegalAccessException ->
                    log.log(Level.SEVERE, "Failed to unfreeze registries", e)
                else -> throw e
            }
        }
    }

    override fun onModuleDisable() = Unit

    fun generateResourcePack(): File? =
        runCatching {
            File("VaneResourcePack.zip").also { file ->
                ResourcePackGenerator().also { pack ->
                    vaneModules.forEach { it.generateResourcePack(pack) }
                    pack.write(file)
                }
            }
        }.onFailure { log.log(Level.SEVERE, "Error while generating resourcepack", it) }.getOrNull()

    fun forAllModuleComponents(f: Consumer1<ModuleComponent<*>?>?) {
        vaneModules.forEach { it.forEachModuleComponent(f) }
    }

    fun itemRegistry(): CustomItemRegistry? = itemRegistry

    fun modelDataRegistry(): CustomModelDataRegistry? = modelDataRegistry

    fun checkForUpdate() {
        if (currentVersion == null) {
            try {
                currentVersion = "v" + Properties().also { props ->
                    props.load(Core::class.java.getResourceAsStream("/vane-core.properties"))
                }.getProperty("version")
            } catch (e: IOException) {
                log.severe("Could not load current version from included properties file: $e")
                return
            }
        }

        try {
            val json = readJsonFromUrl("https://api.github.com/repos/oddlama/vane/releases/latest")
            latestVersion = json.getString("tag_name")
            if (latestVersion != null && latestVersion != currentVersion) {
                log.warning("A newer version of vane is available online! (current=$currentVersion, new=$latestVersion)")
                log.warning("Please update as soon as possible to get the latest features and fixes.")
                log.warning("Get the latest release here: https://github.com/oddlama/vane/releases/latest")
            }
        } catch (e: Exception) {
            when (e) {
                is IOException, is JSONException, is URISyntaxException ->
                    log.warning("Could not check for updates: $e")
                else -> throw e
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    fun onPlayerJoinSendUpdateNotice(event: PlayerJoinEvent) {
        if (!configUpdateNotices) return

        val player = event.player
        // Send an update message if a new version is available and player is OP.
        if (latestVersion != null && latestVersion != currentVersion && player.isOp) {
            // This message is intentionally not translated to ensure it will
            // be displayed correctly and so that everyone understands it.
            player.sendMessage(
                Component.text("A new version of vane ", NamedTextColor.GREEN)
                    .append(Component.text("($latestVersion)", NamedTextColor.AQUA))
                    .append(Component.text(" is available!", NamedTextColor.GREEN))
            )
            player.sendMessage(Component.text("Please update soon to get the latest features.", NamedTextColor.GREEN))
            player.sendMessage(
                Component.text("Click here to go to the download page", NamedTextColor.AQUA)
                    .clickEvent(ClickEvent.openUrl("https://github.com/oddlama/vane/releases/latest"))
            )
        }
    }

    companion object {
        /** Use sparingly. */
        private var INSTANCE: Core? = null

        fun instance(): Core? = INSTANCE
    }
}
