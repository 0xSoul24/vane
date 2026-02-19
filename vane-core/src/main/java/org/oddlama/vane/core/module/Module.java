package org.oddlama.vane.core.module;

import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bstats.bukkit.Metrics;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootTables;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigString;
import org.oddlama.vane.annotation.config.ConfigVersion;
import org.oddlama.vane.annotation.lang.LangVersion;
import org.oddlama.vane.annotation.persistent.Persistent;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.LootTable;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.config.ConfigManager;
import org.oddlama.vane.core.functional.Consumer1;
import org.oddlama.vane.core.lang.LangManager;
import org.oddlama.vane.core.persistent.PersistentStorageManager;
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.oddlama.vane.util.ResourceList.getResources;

public abstract class Module<T extends Module<T>> extends JavaPlugin implements Context<T>, org.bukkit.event.Listener {

	public final VaneModule annotation = getClass().getAnnotation(VaneModule.class);
	public Core core;
	public Logger log = getLogger();
	public ComponentLogger clog = getComponentLogger();
	private final String namespace = "vane_" + annotation.name().replaceAll("[^a-zA-Z0-9_]", "_");

	// Managers
	public ConfigManager configManager = new ConfigManager(this);
	public LangManager langManager = new LangManager(this);
	public PersistentStorageManager persistentStorageManager = new PersistentStorageManager(this);
	private boolean persistentStorageDirty = false;

	// Per module catch-all permissions
	public Permission permissionCommandCatchallModule;
	public Random random = new Random();

	// Permission attachment for console
	private List<String> pendingConsolePermissions = new ArrayList<>();
	public PermissionAttachment consoleAttachment;

	// Version fields for config, lang, storage
	@ConfigVersion
	public long configVersion;

	@LangVersion
	public long langVersion;

	@Persistent
	public long storageVersion;

	// Base configuration
	@ConfigString(def = "inherit", desc = "The language for this module. The corresponding language file must be named lang-{lang}.yml. Specifying 'inherit' will load the value set for vane-core.", metrics = true)
	public String configLang;

	@ConfigBoolean(def = true, desc = "Enable plugin metrics via bStats. You can opt-out here or via the global bStats configuration. All collected information is completely anonymous and publicly available.")
	public boolean configMetricsEnabled;

	// Context<T> interface proxy
	private ModuleGroup<T> contextGroup = new ModuleGroup<>(
			this,
			"",
			"The module will only add functionality if this is set to true.");

	@Override
	public void compile(ModuleComponent<T> component) {
		contextGroup.compile(component);
	}

	@Override
	public void addChild(Context<T> subcontext) {
		if (contextGroup == null) {
			// This happens, when contextGroup (above) is initialized and calls
			// compileSelf(),
			// while will try to register it to the parent context (us), but we fake that
			// anyway.
			return;
		}
		contextGroup.addChild(subcontext);
	}

	@Override
	public Context<T> getContext() {
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T getModule() {
		return (T) this;
	}

	@Override
	public String yamlPath() {
		return "";
	}

	@Override
	public String variableYamlPath(String variable) {
		return variable;
	}

	@Override
	public boolean enabled() {
		return contextGroup.enabled();
	}

	// Callbacks for derived classes
	protected void onModuleLoad() {
	}

	public void onModuleEnable() {
	}

	public void onModuleDisable() {
	}

	public void onConfigChange() {
	}

	public void onGenerateResourcePack() throws IOException {
	}

	public final void forEachModuleComponent(final Consumer1<ModuleComponent<?>> f) {
		contextGroup.forEachModuleComponent(f);
	}

	// Loot modification
	private final Map<NamespacedKey, LootTable> additionalLootTables = new HashMap<>();

	// bStats
	public Metrics metrics;

	public Module() {
		// Get core plugin reference, important for inherited configuration
		// and shared state between vane modules
		if (this.getName().equals("vane-core")) {
			core = (Core) this;
		} else {
			core = (Core) getServer().getPluginManager().getPlugin("vane-core");
		}

		// Create per module command catch-all permission
		permissionCommandCatchallModule = new Permission(
				"vane." + getAnnotationName() + ".commands.*",
				"Allow access to all vane-" + getAnnotationName() + " commands",
				PermissionDefault.FALSE);
		registerPermission(permissionCommandCatchallModule);
	}

	/** The namespace used in resource packs */
	public final String namespace() {
		return namespace;
	}

	@Override
	public final void onLoad() {
		// Create data directory
		if (!getDataFolder().exists()) {
			getDataFolder().mkdirs();
		}

		onModuleLoad();
	}

	@Override
	public final void onEnable() {
		// Create console permission attachment
		consoleAttachment = getServer().getConsoleSender().addAttachment(this);
		for (var perm : pendingConsolePermissions) {
			consoleAttachment.setPermission(perm, true);
		}
		pendingConsolePermissions.clear();

		// Register in core
		core.registerModule(this);

		loadPersistentStorage();
		reloadConfiguration();

		// Schedule persistent storage saving every minute
		scheduleTaskTimer(
				() -> {
					if (persistentStorageDirty) {
						savePersistentStorage();
						persistentStorageDirty = false;
					}
				},
				60 * 20,
				60 * 20);
	}

	@Override
	public void onDisable() {
		disable();

		// Save persistent storage
		savePersistentStorage();

		// Unregister in core
		core.unregisterModule(this);
	}

	@Override
	public void enable() {
		if (configMetricsEnabled) {
			var id = annotation.bstats();
			if (id != -1) {
				metrics = new Metrics(this, id);
				configManager.registerMetrics(metrics);
			}
		}
		onModuleEnable();
		contextGroup.enable();
		registerListener(this);
	}

	@Override
	public void disable() {
		unregisterListener(this);
		contextGroup.disable();
		onModuleDisable();
		metrics = null;
	}

	@Override
	public void configChange() {
		onConfigChange();
		contextGroup.configChange();
	}

	@Override
	public void generateResourcePack(final ResourcePackGenerator pack) throws IOException {
		// Generate language
		final var pattern = Pattern.compile("lang-.*\\.yml");
		Arrays.stream(getDataFolder().listFiles((d, name) -> pattern.matcher(name).matches()))
				.sorted()
				.forEach(langFile -> {
					final var yaml = YamlConfiguration.loadConfiguration(langFile);
					try {
						langManager.generateResourcePack(pack, yaml, langFile);
					} catch (Exception e) {
						throw new RuntimeException(
								"Error while generating language for '" + langFile + "' of module " + getAnnotationName(),
								e);
					}
				});

		// Add files
		final var index = getResource("resource_pack/index");
		if (index != null) {
			try (final var reader = new BufferedReader(new InputStreamReader(index))) {
				String filePath;
				while ((filePath = reader.readLine()) != null) {
					final var content = getResource("resource_pack/" + filePath);
					pack.addFile(filePath, content);
				}
			} catch (IOException e) {
				log.log(Level.SEVERE, "Could not load resource pack index file of module " + getAnnotationName(), e);
			}
		}

		onGenerateResourcePack(pack);
		contextGroup.generateResourcePack(pack);
	}

	private boolean tryReloadConfiguration() {
		// Generate new file if not existing
		final var file = configManager.standardFile();
		if (!file.exists() && !configManager.generateFile(file, null)) {
			return false;
		}

		// Reload automatic variables
		return configManager.reload(file);
	}

	private void updateLangFile(String langFile) {
		final var file = new File(getDataFolder(), langFile);
		final var fileVersion = YamlConfiguration.loadConfiguration(file).getLong("Version", -1);
		long resourceVersion = -1;

		final var res = getResource(langFile);
		try (final var reader = new InputStreamReader(res)) {
			resourceVersion = YamlConfiguration.loadConfiguration(reader).getLong("Version", -1);
		} catch (IOException e) {
			log.log(Level.SEVERE, "Error while updating lang file '" + file + "' of module " + getAnnotationName(), e);
		}

		if (resourceVersion > fileVersion) {
			try {
				Files.copy(getResource(langFile), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				log.log(Level.SEVERE, "Error while copying lang file '" + file + "' of module " + getAnnotationName(), e);
			}
		}
	}

	private boolean tryReloadLocalization() {
		// Copy all embedded lang files if their version is newer.
		getResources(getClass(), Pattern.compile("lang-.*\\.yml")).stream().forEach(this::updateLangFile);

		// Get configured language code
		var langCode = configLang;
		if ("inherit".equals(langCode)) {
			langCode = core.configLang;

			if (langCode == null) {
				// Core failed to load, so the server will be shutdown anyway.
				// Prevent an additional warning by falling back to en.
				langCode = "en";
			} else if ("inherit".equals(langCode)) {
				// Fallback to en in case 'inherit' is used in vane-core.
				langCode = "en";
			}
		}

		// Generate new file if not existing
		final var file = new File(getDataFolder(), "lang-" + langCode + ".yml");
		if (!file.exists()) {
			log.severe("Missing language file '" + file.getName() + "' for module " + getAnnotationName());
			return false;
		}

		// Reload automatic variables
		return langManager.reload(file);
	}

	public boolean reloadConfiguration() {
		boolean wasEnabled = enabled();

		if (!tryReloadConfiguration()) {
			// Force stop server, we encountered an invalid config file
			log.severe("Invalid plugin configuration. Shutting down.");
			getServer().shutdown();
			return false;
		}

		// Reload localization
		if (!tryReloadLocalization()) {
			// Force stop server, we encountered an invalid lang file
			log.severe("Invalid localization file. Shutting down.");
			getServer().shutdown();
			return false;
		}

		if (wasEnabled && !enabled()) {
			// Disable plugin if needed
			disable();
		} else if (!wasEnabled && enabled()) {
			// Enable plugin if needed
			enable();
		}

		configChange();
		return true;
	}

	public File getPersistentStorageFile() {
		// Generate new file if not existing
		return new File(getDataFolder(), "storage.json");
	}

	public void loadPersistentStorage() {
		// Load automatic persistent variables
		final var file = getPersistentStorageFile();
		if (!persistentStorageManager.load(file)) {
			// Force stop server, we encountered an invalid persistent storage file.
			// This prevents further corruption.
			log.severe("Invalid persistent storage. Shutting down to prevent further corruption.");
			getServer().shutdown();
		}
	}

	public void markPersistentStorageDirty() {
		persistentStorageDirty = true;
	}

	public void savePersistentStorage() {
		// Save automatic persistent variables
		final var file = getPersistentStorageFile();
		persistentStorageManager.save(file);
	}

	public void registerListener(Listener listener) {
		getServer().getPluginManager().registerEvents(listener, this);
	}

	public void unregisterListener(Listener listener) {
		HandlerList.unregisterAll(listener);
	}

	public String getAnnotationName() {
		return annotation.name();
	}

	public void registerCommand(Command<?> command) {
		LifecycleEventManager<Plugin> manager = this.getLifecycleManager();
		manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
			event.registrar().register(command.getCommand(), command.langDescription.str(), command.getAliases());
		});
	}

	public void unregisterCommand(Command<?> command) {
		var bukkitCommand = command.getBukkitCommand();
		getServer().getCommandMap().getKnownCommands().values().remove(bukkitCommand);
		bukkitCommand.unregister(getServer().getCommandMap());
	}

	public void addConsolePermission(Permission permission) {
		addConsolePermission(permission.getName());
	}

	public void addConsolePermission(String permission) {
		if (consoleAttachment == null) {
			pendingConsolePermissions.add(permission);
		} else {
			consoleAttachment.setPermission(permission, true);
		}
	}

	public void registerPermission(Permission permission) {
		try {
			getServer().getPluginManager().addPermission(permission);
		} catch (IllegalArgumentException e) {
			log.log(Level.SEVERE, "Permission '" + permission.getName() + "' was already defined", e);
		}
	}

	public void unregisterPermission(Permission permission) {
		getServer().getPluginManager().removePermission(permission);
	}

	public LootTable lootTable(final LootTables table) {
		return lootTable(table.getKey());
	}

	public List<OfflinePlayer> getOfflinePlayersWithValidName() {
		return Arrays.stream(getServer().getOfflinePlayers())
				.filter(k -> k.getName() != null)
				.collect(Collectors.toList());
	}

	public LootTable lootTable(final NamespacedKey key) {
		var additionalLootTable = additionalLootTables.get(key);
		if (additionalLootTable == null) {
			additionalLootTable = new LootTable();
			additionalLootTables.put(key, additionalLootTable);
		}
		return additionalLootTable;
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onModuleLootGenerate(final LootGenerateEvent event) {
		final var lootTable = event.getLootTable();
		// Should never happen because according to the api this is @NotNull,
		// yet it happens for some people that copied their world from singleplayer to
		// the server.
		if (lootTable == null) {
			return;
		}
		final var additionalLootTable = additionalLootTables.get(lootTable.getKey());
		if (additionalLootTable == null) {
			return;
		}

		final var loc = event.getLootContext().getLocation();
		final var localRandom = new Random(
				random.nextInt() +
						(loc.getBlockX() & (0xffff << 16)) +
						(loc.getBlockY() & (0xffff << 32)) +
						(loc.getBlockZ() & (0xffff << 48)));
		additionalLootTable.generateLoot(event.getLoot(), localRandom);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onModulePlayerCaughtFish(final PlayerFishEvent event) {
		// This is a dirty non-commutative way to apply fishing loot tables
		// that skews subtable probabilities,
		// consider somehow programmatically generating datapacks or
		// modifying loot tables directly instead.
		if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
			return;
		}
		if (event.getCaught() instanceof Item itemEntity) {
			final Player player = event.getPlayer();
			final FishHook hookEntity = event.getHook();
			final double playerLuck = player.getAttribute(Attribute.LUCK).getValue();
			final ItemStack rodStack = player.getInventory().getItem(event.getHand());
			final double rodLuck = rodStack.getEnchantmentLevel(Enchantment.LUCK_OF_THE_SEA);  // Can bukkit provide access to fishing_luck_bonus of 1.24 item component system?
			final double totalLuck = playerLuck + rodLuck;
			final double weightFish     =                               Math.max(0, 85 + totalLuck * -1);
			final double weightJunk     =                               Math.max(0, 10 + totalLuck * -2);
			final double weightTreasure = hookEntity.isInOpenWater() ? Math.max(0, 5 + totalLuck * 2) : 0;
			final double roll = random.nextDouble() * (weightFish + weightJunk + weightTreasure);
			NamespacedKey key;
			if (roll < weightFish) {
				key = LootTables.FISHING_FISH.getKey();
			} else if (roll < weightFish + weightJunk) {
				key = LootTables.FISHING_JUNK.getKey();
			} else {
				key = LootTables.FISHING_TREASURE.getKey();
			}
			final var additionalLootTable = additionalLootTables.get(key);
			if (additionalLootTable == null) {
				// Do not modify the caught item
				return;
			}
			final var newItem = additionalLootTable.generateOverride(new Random(random.nextInt()));
			if (newItem == null) {
				return;
			}
			itemEntity.setItemStack(newItem);
		}
	}
}
