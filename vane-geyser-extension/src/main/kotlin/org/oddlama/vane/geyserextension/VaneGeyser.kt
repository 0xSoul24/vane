package org.oddlama.vane.geyserextension

import org.geysermc.event.subscribe.Subscribe
import org.geysermc.geyser.api.event.lifecycle.*
import org.geysermc.geyser.api.extension.Extension

/**
 * Main entry point for the Vane Geyser extension.
 *
 * This class implements [Extension] and acts as the lifecycle manager for the
 * vane-geyser-extension module. It subscribes to Geyser lifecycle events to:
 * - Log extension metadata during pre-initialization.
 * - Track loaded resource packs.
 * - Delegate command and custom item registration to [CommandsRegistration] and [ItemRegistration].
 *
 * The extension is declared in `extension.yml` and instantiated by the Geyser extension loader.
 */
class VaneGeyser : Extension {
    /**
     * Handles the [GeyserPreInitializeEvent] to print a startup banner.
     *
     * Logs the extension name, version, authors, and a brief description
     * to the Geyser console before the server finishes initialization.
     */
    @Subscribe
    fun onGeyserPreInitializeEvent(event: GeyserPreInitializeEvent) {
        val desc = description()
        logger().info("")
        logger().info("##############################################")
        logger().info("Extension: ${desc.name()}")
        logger().info("Version: ${desc.version()}")
        logger().info("Authors: ${desc.authors().joinToString(", ")}")
        logger().info("Description: A plugin-suite that provides many immersive and lore-friendly additions to vanilla Minecraft.")
        logger().info("##############################################")
        logger().info("")
    }

    /**
     * Handles the [GeyserDefineResourcePacksEvent] to log resource pack loading.
     *
     * Logs the number of resource packs currently being loaded by Geyser.
     * Additional resource packs could be registered via [GeyserDefineResourcePacksEvent.register]
     * if needed.
     */
    @Subscribe
    fun onGeyserDefineResourcePacksEvent(event: GeyserDefineResourcePacksEvent) {
        logger().info("Loading: ${event.resourcePacks().size} resource packs.")
    }

    /**
     * Handles the [GeyserPostInitializeEvent] fired after Geyser is fully initialized.
     *
     * Logs the extension name and its data folder path once Geyser is ready
     * to accept Bedrock player connections.
     */
    @Subscribe
    fun onPostInitialize(event: GeyserPostInitializeEvent?) {
        with(logger()) {
            info("Loading ${description().name()}...")
            info("${dataFolder()}")
        }
    }

    /**
     * Handles the [GeyserPreReloadEvent] to support extension reloading.
     *
     * Logs a reload message when the Geyser reload cycle is triggered.
     * Extension configuration could be re-read here if needed.
     *
     * Note: Geyser's extension template declares this handler with a
     * [GeyserPreInitializeEvent] parameter while its documentation points at the reload event.
     * Following the parameter rather than the docs subscribes a second handler to
     * pre-initialize, so every startup logs a "Reloading" line and no reload is ever observed.
     */
    @Subscribe
    fun onGeyserReload(event: GeyserPreReloadEvent) {
        logger().info("Reloading ${description().name()}!")
    }

    /**
     * Handles the [GeyserDefineCommandsEvent] to register Bedrock-specific commands.
     *
     * Delegates to [CommandsRegistration] which registers the `/menu` command
     * and all associated Bedrock form-based UI menus.
     */
    @Subscribe
    fun onGeyserDefineCommands(event: GeyserDefineCommandsEvent) {
        CommandsRegistration.onGeyserDefineCommands(event, this)
    }

    /**
     * Handles the [GeyserDefineCustomItemsEvent] to register custom item definitions.
     *
     * Delegates to [ItemRegistration] which maps vane's custom Java items
     * (tomes, sickles, scrolls, etc.) to their Bedrock resource pack counterparts.
     */
    @Subscribe
    fun onGeyserDefineCustomItems(event: GeyserDefineCustomItemsEvent) {
        ItemRegistration.onGeyserDefineCustomItems(event)
    }
}
