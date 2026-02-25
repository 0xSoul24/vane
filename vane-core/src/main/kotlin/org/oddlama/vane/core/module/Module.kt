package org.oddlama.vane.core.module

import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager
import io.papermc.paper.plugin.lifecycle.event.handler.LifecycleEventHandler
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import org.bstats.bukkit.Metrics
import org.bukkit.NamespacedKey
import org.bukkit.OfflinePlayer
import org.bukkit.attribute.Attribute
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Item
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.event.world.LootGenerateEvent
import org.bukkit.loot.LootTables
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionAttachment
import org.bukkit.permissions.PermissionDefault
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import org.oddlama.vane.annotation.VaneModule
import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.annotation.config.ConfigString
import org.oddlama.vane.annotation.config.ConfigVersion
import org.oddlama.vane.annotation.lang.LangVersion
import org.oddlama.vane.annotation.persistent.Persistent
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.LootTable
import org.oddlama.vane.core.command.Command
import org.oddlama.vane.core.config.ConfigManager
import org.oddlama.vane.core.functional.Consumer1
import org.oddlama.vane.core.lang.LangManager
import org.oddlama.vane.core.persistent.PersistentStorageManager
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator
import org.oddlama.vane.util.ResourceList
import java.io.*
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import java.util.regex.Pattern
import java.util.stream.Collectors
import kotlin.math.max

abstract class Module<T : Module<T?>?> : JavaPlugin(), Context<T?>, Listener {
    val annotation: VaneModule = javaClass.getAnnotation(VaneModule::class.java)!!
    var core: Core? = null
    var log: Logger = logger
    var clog: ComponentLogger = componentLogger
    private val namespace = "vane_" + annotation.name.replace("[^a-zA-Z0-9_]".toRegex(), "_")

    // Managers
    var configManager: ConfigManager = ConfigManager(this)
    var langManager: LangManager = LangManager(this)
    var persistentStorageManager: PersistentStorageManager = PersistentStorageManager(this)
    private var persistentStorageDirty = false

    // Per module catch-all permissions
    var permissionCommandCatchallModule: Permission?
    var random: Random = Random()

    // Permission attachment for console
    private val pendingConsolePermissions: MutableList<String> = ArrayList<String>()
    var consoleAttachment: PermissionAttachment? = null

    // Version fields for config, lang, storage
    @ConfigVersion
    var configVersion: Long = 0

    @LangVersion
    var langVersion: Long = 0

    @Persistent
    var storageVersion: Long = 0

    // Base configuration
    @ConfigString(
        def = "inherit",
        desc = "The language for this module. The corresponding language file must be named lang-{lang}.yml. Specifying 'inherit' will load the value set for vane-core.",
        metrics = true
    )
    var configLang: String? = null

    @ConfigBoolean(
        def = true,
        desc = "Enable plugin metrics via bStats. You can opt-out here or via the global bStats configuration. All collected information is completely anonymous and publicly available."
    )
    var configMetricsEnabled: Boolean = false

    // Context<T> interface proxy
    private val contextGroup: ModuleGroup<T?> = ModuleGroup<T?>(
        this,
        "",
        "The module will only add functionality if this is set to true.",
        false
    )

    override fun compile(component: ModuleComponent<T?>?) {
        contextGroup.compile(component)
    }

    override fun addChild(subcontext: Context<T?>?) {
        // Prevent adding the module's root group as a child of itself which would create
        // a self-referential cycle leading to infinite recursion on enable/disable.
        if (subcontext === contextGroup) return
        contextGroup.addChild(subcontext)
    }

    override val context: Context<T?>?
        get() = this

    @Suppress("UNCHECKED_CAST")
    override val module: T?
        get() = this as T

    override fun yamlPath(): String? {
        return ""
    }

    override fun variableYamlPath(variable: String?): String? {
        return variable
    }

    override fun enabled(): Boolean {
        return contextGroup.enabled()
    }

    // Callbacks for derived classes
    protected fun onModuleLoad() {
    }

    override fun onModuleEnable() {
    }

    override fun onModuleDisable() {
    }

    override fun onConfigChange() {
    }

    @Throws(IOException::class)
    fun onGenerateResourcePack() {
    }

    override fun forEachModuleComponent(f: Consumer1<ModuleComponent<*>?>?) {
        contextGroup.forEachModuleComponent(f)
    }

    // Loot modification
    private val additionalLootTables: MutableMap<NamespacedKey?, LootTable?> = HashMap<NamespacedKey?, LootTable?>()

    // bStats
    var metrics: Metrics? = null

    init {
        // Get core plugin reference, important for inherited configuration
        // and shared state between vane modules
        core = if (this.name == "vane-core") {
            this as Core
        } else {
            server.pluginManager.getPlugin("vane-core") as Core?
        }

        // Create per module command catch-all permission
        permissionCommandCatchallModule = Permission(
            "vane." + this.annotationName + ".commands.*",
            "Allow access to all vane-" + this.annotationName + " commands",
            PermissionDefault.FALSE
        )
        registerPermission(permissionCommandCatchallModule!!)

        // Compile the context group now that the Module initialization has finished
        contextGroup.compileSelf()
    }

    /** The namespace used in resource packs  */
    override fun namespace(): String {
        return namespace
    }

    override fun onLoad() {
        // Create data directory
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }

        onModuleLoad()
    }

    override fun onEnable() {
        // Create console permission attachment
        consoleAttachment = server.consoleSender.addAttachment(this)
        for (perm in pendingConsolePermissions) {
            consoleAttachment!!.setPermission(perm, true)
        }
        pendingConsolePermissions.clear()

        // Register in core
        core!!.registerModule(this)

        loadPersistentStorage()
        reloadConfiguration()

        // Schedule persistent storage saving every minute
        scheduleTaskTimer(
            {
                if (persistentStorageDirty) {
                    savePersistentStorage()
                    persistentStorageDirty = false
                }
            },
            (60 * 20).toLong(),
            (60 * 20).toLong()
        )
    }

    override fun onDisable() {
        disable()

        // Save persistent storage
        savePersistentStorage()

        // Unregister in core
        core!!.unregisterModule(this)
    }

    override fun enable() {
        if (configMetricsEnabled) {
            val id = annotation.bstats
            if (id != -1) {
                metrics = Metrics(this, id)
                configManager.registerMetrics(metrics)
            }
        }
        onModuleEnable()
        contextGroup.enable()
        registerListener(this)
    }

    override fun disable() {
        unregisterListener(this)
        contextGroup.disable()
        onModuleDisable()
        metrics = null
    }

    override fun configChange() {
        onConfigChange()
        contextGroup.configChange()
    }

    @Throws(IOException::class)
    override fun generateResourcePack(pack: ResourcePackGenerator?) {
        if (pack == null) return
        // Generate language
        val pattern = Pattern.compile("lang-.*\\.yml")
        val langFiles = dataFolder.listFiles { _, name -> name != null && pattern.matcher(name).matches() } ?: arrayOf()
        Arrays.stream(langFiles)
            .sorted()
            .forEach { langFile: File? ->
                if (langFile == null) return@forEach
                val yaml = YamlConfiguration.loadConfiguration(langFile)
                try {
                    val res = getResource(langFile.name)
                    if (res != null) {
                        langManager.generateResourcePack(pack, yaml, langFile)
                    } else {
                        // Missing embedded resource, but YAML exists on disk; still attempt generation via YAML
                        langManager.generateResourcePack(pack, yaml, langFile)
                    }
                } catch (e: Exception) {
                    throw RuntimeException(
                        "Error while generating language for '" + langFile + "' of module " + this.annotationName,
                        e
                    )
                }
            }

        // Add files
        val index = getResource("resource_pack/index")
        if (index != null) {
            try {
                BufferedReader(InputStreamReader(index)).use { reader ->
                    var filePath: String?
                    while ((reader.readLine().also { filePath = it }) != null) {
                        if (filePath == null) continue
                        val content = getResource("resource_pack/$filePath")
                        if (content != null) {
                            pack.addFile(filePath, content)
                        } else {
                            log.log(Level.WARNING, "Missing resource 'resource_pack/" + filePath + "' in module " + this.annotationName)
                        }
                    }
                }
            } catch (e: IOException) {
                log.log(Level.SEVERE, "Could not load resource pack index file of module " + this.annotationName, e)
            }
        }

        onGenerateResourcePack(pack)
        contextGroup.generateResourcePack(pack)
    }

    private fun tryReloadConfiguration(): Boolean {
        // Generate new file if not existing
        val file = configManager.standardFile()
        if (!file.exists() && !configManager.generateFile(file, null)) {
            return false
        }

        // Reload automatic variables
        return configManager.reload(file)
    }

    private fun updateLangFile(langFile: String) {
        val file = File(dataFolder, langFile)
        val fileVersion = YamlConfiguration.loadConfiguration(file).getLong("Version", -1)
        var resourceVersion: Long = -1

        val res = getResource(langFile)
        try {
            if (res != null) {
                InputStreamReader(res).use { reader ->
                    resourceVersion = YamlConfiguration.loadConfiguration(reader).getLong("Version", -1)
                }
            }
        } catch (e: IOException) {
            log.log(Level.SEVERE, "Error while updating lang file '" + file + "' of module " + this.annotationName, e)
        }

        if (resourceVersion > fileVersion) {
            try {
                val res2 = getResource(langFile)
                if (res2 != null) {
                    Files.copy(res2, file.toPath(), StandardCopyOption.REPLACE_EXISTING)
                } else {
                    log.log(Level.WARNING, "Embedded lang resource '" + langFile + "' missing for module " + this.annotationName)
                }
            } catch (e: IOException) {
                log.log(
                    Level.SEVERE,
                    "Error while copying lang file '" + file + "' of module " + this.annotationName,
                    e
                )
            }
        }
    }

    private fun tryReloadLocalization(): Boolean {
        // Copy all embedded lang files if their version is newer.
        ResourceList.getResources(javaClass, Pattern.compile("lang-.*\\.yml")).stream()
            .forEach { langFile: String? -> if (langFile != null) this.updateLangFile(langFile) }

        // Get configured language code
        var langCode = configLang
        if ("inherit" == langCode) {
            langCode = core!!.configLang

            if (langCode == null) {
                // Core failed to load, so the server will be shutdown anyway.
                // Prevent an additional warning by falling back to en.
                langCode = "en"
            } else if ("inherit" == langCode) {
                // Fallback to en in case 'inherit' is used in vane-core.
                langCode = "en"
            }
        }

        // Generate new file if not existing
        val file = File(dataFolder, "lang-$langCode.yml")
        if (!file.exists()) {
            log.severe("Missing language file '" + file.getName() + "' for module " + this.annotationName)
            return false
        }

        // Reload automatic variables
        return langManager.reload(file)
    }

    fun reloadConfiguration(): Boolean {
        val wasEnabled = enabled()

        if (!tryReloadConfiguration()) {
            // Force stop server, we encountered an invalid config file
            log.severe("Invalid plugin configuration. Shutting down.")
            server.shutdown()
            return false
        }

        // Reload localization
        if (!tryReloadLocalization()) {
            // Force stop server, we encountered an invalid lang file
            log.severe("Invalid localization file. Shutting down.")
            server.shutdown()
            return false
        }

        if (wasEnabled && !enabled()) {
            // Disable plugin if needed
            disable()
        } else if (!wasEnabled && enabled()) {
            // Enable plugin if needed
            enable()
        }

        configChange()
        return true
    }

    val persistentStorageFile: File
        get() =// Generate new file if not existing
            File(dataFolder, "storage.json")

    fun loadPersistentStorage() {
        // Load automatic persistent variables
        val file: File = this.persistentStorageFile
        if (!persistentStorageManager.load(file)) {
            // Force stop server, we encountered an invalid persistent storage file.
            // This prevents further corruption.
            log.severe("Invalid persistent storage. Shutting down to prevent further corruption.")
            server.shutdown()
        }
    }

    override fun markPersistentStorageDirty() {
        persistentStorageDirty = true
    }

    fun savePersistentStorage() {
        // Save automatic persistent variables
        val file: File = this.persistentStorageFile
        persistentStorageManager.save(file)
    }

    fun registerListener(listener: Listener) {
        server.pluginManager.registerEvents(listener, this)
    }

    fun unregisterListener(listener: Listener) {
        HandlerList.unregisterAll(listener)
    }

    val annotationName: String
        get() = annotation.name

    fun registerCommand(command: Command<*>) {
        val manager: LifecycleEventManager<Plugin> = this.lifecycleManager
        manager.registerEventHandler(
            LifecycleEvents.COMMANDS,
            LifecycleEventHandler { event: ReloadableRegistrarEvent<Commands>? ->
                event!!.registrar()
                    .register(command.command, command.langDescription.str(), command.getAliases())
            }
        )
    }

    fun unregisterCommand(command: Command<*>) {
        val bukkitCommand = command.getBukkitCommand()
        server.commandMap.knownCommands.values.remove(bukkitCommand)
        bukkitCommand.unregister(server.commandMap)
    }

    fun addConsolePermission(permission: Permission) {
        addConsolePermission(permission.name)
    }

    fun addConsolePermission(permission: String) {
        if (consoleAttachment == null) {
            pendingConsolePermissions.add(permission)
        } else {
            consoleAttachment!!.setPermission(permission, true)
        }
    }

    fun registerPermission(permission: Permission) {
        try {
            server.pluginManager.addPermission(permission)
        } catch (e: IllegalArgumentException) {
            log.log(Level.SEVERE, "Permission '" + permission.name + "' was already defined", e)
        }
    }

    fun unregisterPermission(permission: Permission) {
        server.pluginManager.removePermission(permission)
    }

    fun lootTable(table: LootTables): LootTable {
        return lootTable(table.key)
    }

    val offlinePlayersWithValidName: MutableList<OfflinePlayer?>
        get() = Arrays.stream(server.offlinePlayers)
            .filter { k: OfflinePlayer? -> k!!.name != null }
            .collect(Collectors.toList())

    fun lootTable(key: NamespacedKey?): LootTable {
        var additionalLootTable = additionalLootTables[key]
        if (additionalLootTable == null) {
            additionalLootTable = LootTable()
            additionalLootTables[key] = additionalLootTable
        }
        return additionalLootTable
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onModuleLootGenerate(event: LootGenerateEvent) {
        val lootTable = event.lootTable
        // Should never happen because according to the api this is @NotNull,
        // yet it happens for some people that copied their world from singleplayer to
        // the server.
        val additionalLootTable = additionalLootTables[lootTable.key] ?: return

        val loc = event.lootContext.location
        val localRandom = Random(
            (random.nextInt() +
                    (loc.blockX and (0xffff shl 16)) +
                    (loc.blockY and (0xffff shl 32)) +
                    (loc.blockZ and (0xffff shl 48))).toLong()
        )
        additionalLootTable.generateLoot(event.loot, localRandom)
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onModulePlayerCaughtFish(event: PlayerFishEvent) {
        // This is a dirty non-commutative way to apply fishing loot tables
        // that skews subtable probabilities,
        // consider somehow programmatically generating datapacks or
        // modifying loot tables directly instead.
        if (event.state != PlayerFishEvent.State.CAUGHT_FISH) {
            return
        }
        if (event.caught is Item) {
            val player = event.getPlayer()
            val hookEntity = event.hook
            val playerLuck = player.getAttribute(Attribute.LUCK)!!.value
            val rodStack = player.inventory.getItem(event.hand!!)
            val rodLuck = rodStack.getEnchantmentLevel(Enchantment.LUCK_OF_THE_SEA)
                .toDouble() // Can bukkit provide access to fishing_luck_bonus of 1.24 item component system?
            val totalLuck = playerLuck + rodLuck
            val weightFish = max(0.0, 85 + totalLuck * -1)
            val weightJunk = max(0.0, 10 + totalLuck * -2)
            val weightTreasure = if (hookEntity.isInOpenWater) max(0.0, 5 + totalLuck * 2) else 0.0
            val roll = random.nextDouble() * (weightFish + weightJunk + weightTreasure)
            val key = if (roll < weightFish) {
                LootTables.FISHING_FISH.key
            } else if (roll < weightFish + weightJunk) {
                LootTables.FISHING_JUNK.key
            } else {
                LootTables.FISHING_TREASURE.key
            }
            val additionalLootTable = additionalLootTables[key] ?: // Do not modify the caught item
            return
            val newItem = additionalLootTable.generateOverride(Random(random.nextInt().toLong())) ?: return
            val itemEntity = event.caught as Item
            itemEntity.itemStack = newItem
         }
     }
 }
