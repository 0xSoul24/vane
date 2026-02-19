package org.oddlama.vane.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.json.JSONException;
import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.enchantments.EnchantmentManager;
import org.oddlama.vane.core.functional.Consumer1;
import org.oddlama.vane.core.item.CustomItemRegistry;
import org.oddlama.vane.core.item.CustomModelDataRegistry;
import org.oddlama.vane.core.item.DurabilityManager;
import org.oddlama.vane.core.item.ExistingItemConverter;
import org.oddlama.vane.core.item.VanillaFunctionalityInhibitor;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.menu.MenuManager;
import org.oddlama.vane.core.misc.AuthMultiplexer;
import org.oddlama.vane.core.misc.CommandHider;
import org.oddlama.vane.core.misc.HeadLibrary;
import org.oddlama.vane.core.misc.LootChestProtector;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.module.ModuleComponent;
import org.oddlama.vane.core.resourcepack.ResourcePackDistributor;
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;

import static org.oddlama.vane.util.Conversions.msToTicks;
import static org.oddlama.vane.util.IOUtil.readJsonFromUrl;

@VaneModule(name = "core", bstats = 8637, configVersion = 6, langVersion = 5, storageVersion = 1)
public class Core extends Module<Core> {

    /** Use sparingly. */
    private static Core INSTANCE = null;

    public static Core instance() {
        return INSTANCE;
    }

    public EnchantmentManager enchantmentManager;
    private CustomModelDataRegistry modelDataRegistry;
    private CustomItemRegistry itemRegistry;

    @ConfigBoolean(
        def = true,
        desc = "Allow loading of player heads in relevant menus. Disabling this will show all player heads using the Steve skin, which may perform better on low-performance servers and clients."
    )
    public boolean configPlayerHeadsInMenus;

    @LangMessage
    public TranslatedMessage langCommandNotAPlayer;

    @LangMessage
    public TranslatedMessage langCommandPermissionDenied;

    @LangMessage
    public TranslatedMessage langInvalidTimeFormat;

    // Module registry
    private SortedSet<Module<?>> vaneModules = new TreeSet<>((a, b) -> a.getAnnotationName().compareTo(b.getAnnotationName()));

    public final ResourcePackDistributor resourcePackDistributor;

    public void registerModule(Module<?> module) {
        vaneModules.add(module);
    }

    public void unregisterModule(Module<?> module) {
        vaneModules.remove(module);
    }

    public SortedSet<Module<?>> getModules() {
        return Collections.unmodifiableSortedSet(vaneModules);
    }

    // Vane global command catch-all permission
    public Permission permissionCommandCatchall = new Permission(
        "vane.*.commands.*",
        "Allow access to all vane commands (ONLY FOR ADMINS!)",
        PermissionDefault.FALSE
    );

    public MenuManager menuManager;

    // core-config
    @ConfigBoolean(
        def = true,
        desc = "Let the client translate messages using the generated resource pack. This allows every player to select their preferred language, and all plugin messages will also be translated. Disabling this won't allow you to skip generating the resource pack, as it will be needed for custom item textures."
    )
    public boolean configClientSideTranslations;

    @ConfigBoolean(def = true, desc = "Send update notices to OPed player when a new version of vane is available.")
    public boolean configUpdateNotices;

    public String currentVersion = null;
    public String latestVersion = null;

    public Core() {
        if (INSTANCE != null) {
            throw new IllegalStateException("Cannot instanciate Core twice.");
        }
        INSTANCE = this;

        // Create global command catch-all permission
        registerPermission(permissionCommandCatchall);

        // Allow registration of new enchantments and entities
        unfreezeRegistries();

        // Components
        enchantmentManager = new EnchantmentManager(this);
        new HeadLibrary(this);
        new AuthMultiplexer(this);
        new LootChestProtector(this);
        new VanillaFunctionalityInhibitor(this);
        new DurabilityManager(this);
        new org.oddlama.vane.core.commands.Vane(this);
        new org.oddlama.vane.core.commands.CustomItem(this);
        new org.oddlama.vane.core.commands.Enchant(this);
        menuManager = new MenuManager(this);
        resourcePackDistributor = new ResourcePackDistributor(this);
        new CommandHider(this);
        modelDataRegistry = new CustomModelDataRegistry();
        itemRegistry = new CustomItemRegistry();
        new ExistingItemConverter(this);
    }

    @Override
    public void onModuleEnable() {
        if (configUpdateNotices) {
            // Now, and every hour after that, check if a new version is available.
            // OPs will get a message about this when they join.
            scheduleTaskTimer(this::checkForUpdate, 1l, msToTicks(2 * 60l * 60l * 1000l));
        }
    }

    public void unfreezeRegistries() {
        // NOTE: MAGIC VALUES! Introduced for 1.18.2 when registries were frozen. Sad, no workaround
        // at the time.
        try {
            // Make relevant fields accessible
            final var frozen = MappedRegistry.class.getDeclaredField("frozen"/* frozen */);
            frozen.setAccessible(true);
            final var intrusiveHolderCache =
                MappedRegistry.class.getDeclaredField(
                        "unregisteredIntrusiveHolders"/* unregisteredIntrusiveHolders (1.19.3+), intrusiveHolderCache (until 1.19.2) */
                    );
            intrusiveHolderCache.setAccessible(true);

            // Unfreeze required registries
            frozen.set(BuiltInRegistries.ENTITY_TYPE, false);
            intrusiveHolderCache.set(
                BuiltInRegistries.ENTITY_TYPE,
                new IdentityHashMap<EntityType<?>, Holder.Reference<EntityType<?>>>()
            );
            // Since 1.20.2 this is also needed for enchantments:
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            log.log(Level.SEVERE, "Failed to unfreeze registries", e);
        }
    }

    @Override
    public void onModuleDisable() {}

    public File generateResourcePack() {
        try {
            var file = new File("VaneResourcePack.zip");
            var pack = new ResourcePackGenerator();

            for (var m : vaneModules) {
                m.generateResourcePack(pack);
            }

            pack.write(file);
            return file;
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error while generating resourcepack", e);
            return null;
        }
    }

    public void forAllModuleComponents(final Consumer1<ModuleComponent<?>> f) {
        for (var m : vaneModules) {
            m.forEachModuleComponent(f);
        }
    }

    public CustomItemRegistry itemRegistry() {
        return itemRegistry;
    }

    public CustomModelDataRegistry modelDataRegistry() {
        return modelDataRegistry;
    }

    public void checkForUpdate() {
        if (currentVersion == null) {
            try {
                Properties properties = new Properties();
                properties.load(Core.class.getResourceAsStream("/vane-core.properties"));
                currentVersion = "v" + properties.getProperty("version");
            } catch (IOException e) {
                log.severe("Could not load current version from included properties file: " + e);
                return;
            }
        }

        try {
            final var json = readJsonFromUrl("https://api.github.com/repos/oddlama/vane/releases/latest");
            latestVersion = json.getString("tag_name");
            if (latestVersion != null && !latestVersion.equals(currentVersion)) {
                log.warning(
                    "A newer version of vane is available online! (current=" +
                            currentVersion +
                    ", new=" +
                            latestVersion +
                    ")"
                );
                log.warning("Please update as soon as possible to get the latest features and fixes.");
                log.warning("Get the latest release here: https://github.com/oddlama/vane/releases/latest");
            }
        } catch (IOException | JSONException | URISyntaxException e) {
            log.warning("Could not check for updates: " + e);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerJoinSendUpdateNotice(PlayerJoinEvent event) {
        if (!configUpdateNotices) {
            return;
        }

        // Send an update message if a new version is available and player is OP.
        if (latestVersion != null && !latestVersion.equals(currentVersion) && event.getPlayer().isOp()) {
            // This message is intentionally not translated to ensure it will
            // be displayed correctly and so that everyone understands it.
            event
                .getPlayer()
                .sendMessage(
                    Component.text("A new version of vane ", NamedTextColor.GREEN)
                        .append(Component.text("(" + latestVersion + ")", NamedTextColor.AQUA))
                        .append(Component.text(" is available!", NamedTextColor.GREEN))
                );
            event
                .getPlayer()
                .sendMessage(Component.text("Please update soon to get the latest features.", NamedTextColor.GREEN));
            event
                .getPlayer()
                .sendMessage(
                    Component.text("Click here to go to the download page", NamedTextColor.AQUA).clickEvent(
                        ClickEvent.openUrl("https://github.com/oddlama/vane/releases/latest")
                    )
                );
        }
    }
}
