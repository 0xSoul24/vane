package org.oddlama.vane.core.resourcepack;

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import io.papermc.paper.event.connection.configuration.PlayerConnectionReconfigureEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleGroup;
import org.oddlama.vane.util.Nms;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class ResourcePackDistributor extends Listener<Core> {

    // Assume debug environment if both add-plugin and vane-debug are defined, until run-paper adds
    // a better way.
    // https://github.com/jpenilla/run-paper/issues/14
    private static final boolean localDev =
        Nms.serverHandle().options.hasArgument("add-plugin") && Boolean.getBoolean("disable.watchdog");

    @ConfigBoolean(
        def = true,
        desc = "Kick players if they deny to use the specified resource pack (if set)."
    )
    public boolean configForce;

    @LangMessage
    public TranslatedMessage langPackRequired;

    @LangMessage
    public TranslatedMessage langPackSuggested;

    public String packUrl = null;
    public String packSha1 = null;
    public UUID packUuid = UUID.fromString("fbba121a-8f87-4e97-922d-2059777311bf");
    public int counter = 0;

    public CustomResourcePackConfig customResourcePackConfig;
    private ResourcePackFileWatcher fileWatcher;
    private ResourcePackDevServer devServer;

    private final ConcurrentHashMap<UUID, CountDownLatch> latches = new ConcurrentHashMap<>();

    public ResourcePackDistributor(Context<Core> context) {
        super(context.group("ResourcePack", "Enable resource pack distribution."));

        customResourcePackConfig = new CustomResourcePackConfig(getContext());
    }

    @Override
    public void onEnable() {
        if (localDev) {
            try {
                File packOutput = new File("VaneResourcePack.zip");
                if (!packOutput.exists()) {
                    getModule().log.info("Resource Pack Missing, first run? Generating resource pack.");
                    packOutput = getModule().generateResourcePack();
                }
                fileWatcher = new ResourcePackFileWatcher(this, packOutput);
                devServer = new ResourcePackDevServer(this, packOutput);
                devServer.serve();
                fileWatcher.watchForChanges();
            } catch (IOException | InterruptedException e) {
                getModule().log.log(
                    java.util.logging.Level.SEVERE,
                    "Failed to initialize resource pack dev server or file watcher",
                    e
                );
            }

            getModule().log.info("Setting up dev lazy server");
        } else if (((ModuleGroup<Core>) customResourcePackConfig.getContext()).configEnabled) {
            getModule().log.info("Serving custom resource pack");
            packUrl = customResourcePackConfig.configUrl;
            packSha1 = customResourcePackConfig.configSha1;
            packUuid = UUID.fromString(customResourcePackConfig.configUuid);
        } else {
            getModule().log.info("Serving official vane resource pack");
            try {
                Properties properties = new Properties();
                properties.load(Core.class.getResourceAsStream("/vane-core.properties"));
                packUrl = properties.getProperty("resourcePackUrl");
                packSha1 = properties.getProperty("resourcePackSha1");
                packUuid = UUID.fromString(properties.getProperty("resourcePackUuid"));
            } catch (IOException e) {
                getModule().log.severe("Could not load official resource pack sha1 from included properties file");
                packUrl = "";
                packSha1 = "";
                packUuid = UUID.randomUUID();
            }
        }

        // Check sha1 sum validity
        if (packSha1.length() != 40) {
            getModule()
                .log.warning(
                    "Invalid resource pack SHA-1 sum '" +
                            packSha1 +
                    "', should be 40 characters long but has " +
                    packSha1.length() +
                    " characters"
                );
            getModule().log.warning("Disabling resource pack serving and message delaying");

            // Disable resource pack
            packUrl = "";
        }

        // Propagate enable after determining whether the player message delayer is active,
        // so it is only enabled when needed.
        super.onEnable();

        packSha1 = packSha1.toLowerCase();
        if (!packUrl.isEmpty()) {
            // Check if the server has a manually configured resource pack.
            // This would conflict.
            Nms.serverHandle()
                .settings.getProperties()
                .serverResourcePackInfo.ifPresent(rpInfo -> {
                    if (!rpInfo.url().trim().isEmpty()) {
                        getModule()
                            .log.warning(
                                "You have manually configured a resource pack in your server.properties. This cannot be used together with vane, as servers only allow serving a single resource pack."
                            );
                    }
                });

            getModule().log.info("Distributing resource pack from '" + packUrl + "' with sha1 " + packSha1);
        }
    }

    @EventHandler
    public void onPlayerAsyncConnectionConfigure(AsyncPlayerConnectionConfigureEvent event) {
        var profileUuid = event.getConnection().getProfile().getId();
        if (profileUuid == null) { return; }

        // Block the thread to prevent the question screen from going away
        var latch = new CountDownLatch(1);
        var oldLatch = latches.put(profileUuid, latch);
        if (oldLatch != null) {
            oldLatch.countDown(); // Unblock thread that might still be waiting
        }

        sendResourcePackDuringConfiguration(event.getConnection());

        try {
            latch.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            getModule().log.warning("Resource pack wait interrupted for player " + profileUuid);
        }

        event.getConnection().completeReconfiguration();
    }

    @EventHandler
    public void onPlayerConnectionReconfigure(PlayerConnectionReconfigureEvent event) {
        sendResourcePackDuringConfiguration(event.getConnection());
    }

    @EventHandler
    public void onPlayerConnectionClose(PlayerConnectionCloseEvent event) {
        // Cleanup
        Optional.ofNullable(latches.remove(event.getPlayerUniqueId())).ifPresent(CountDownLatch::countDown);
    }

    public void sendResourcePackDuringConfiguration(@NotNull PlayerConfigurationConnection connection) {
        var info = ResourcePackInfo.resourcePackInfo(packUuid, URI.create(packUrl), packSha1);
        var promptLang = (configForce) ? langPackRequired : langPackSuggested;
        var prompt = promptLang.str().isEmpty() ? null : promptLang.strComponent();
        var request = ResourcePackRequest.resourcePackRequest()
            .required(configForce).replace(true)
            .packs(info).callback((uuid, status, audience) -> {
                if (!status.intermediate()) {
                    Optional.ofNullable(latches.remove(connection.getProfile().getId())).ifPresent(CountDownLatch::countDown);
                }
            })
            .prompt(prompt)
            .build();

        connection.getAudience().sendResourcePacks(request);
    }

    // For sending the resource pack during gameplay
    public void sendResourcePack(@NotNull Audience audience) {
        var url2 = packUrl;
        if (localDev) {
            url2 = packUrl + "?" + counter;
            audience.sendMessage(Component.text(url2 + " " + packSha1));
        }

        try {
            ResourcePackInfo info = ResourcePackInfo.resourcePackInfo(packUuid, new URI(url2), packSha1);
            audience.sendResourcePacks(ResourcePackRequest.resourcePackRequest().packs(info).asResourcePackRequest());
        } catch (URISyntaxException e) {
            getModule().log.warning("The provided resource pack URL is incorrect: " + url2);
        }
    }

    @SuppressWarnings({ "deprecation", "UnstableApiUsage" })
    public void updateSha1(File file) {
        if (!localDev) return;
        try {
            var hash = Files.asByteSource(file).hash(Hashing.sha1());
            ResourcePackDistributor.this.packSha1 = hash.toString();
        } catch (IOException ignored) {}
    }
}
