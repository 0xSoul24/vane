package org.oddlama.vane.trifles

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.util.Nms

class RecipeUnlock(context: Context<Trifles?>) :
    Listener<Trifles?>(context.group("RecipeUnlock", "Unlocks all recipes when a player joins.")) {
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val count = Nms.unlockAllRecipes(event.getPlayer())
        if (count > 0) {
            module!!.log.info(
                "Given " +
                        count +
                        " recipes to " +
                        LegacyComponentSerializer.legacySection().serialize(event.getPlayer().displayName())
            )
        }
    }
}
