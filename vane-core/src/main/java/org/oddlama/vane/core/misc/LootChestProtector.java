package org.oddlama.vane.core.misc;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.loot.Lootable;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;

public class LootChestProtector extends Listener<Core> {

    // Prevent loot chest destruction
    private final Map<Block, Map<UUID, Long>> lootBreakAttempts = new HashMap<>();

    // TODO(legacy): this should become a separate group instead of having
    // this boolean.
    @ConfigBoolean(
        def = true,
        desc = "Prevent players from breaking blocks with loot-tables (like treasure chests) when they first attempt to destroy it. They still can break it, but must do so within a short timeframe."
    )
    public boolean configWarnBreakingLootBlocks;

    @LangMessage
    public TranslatedMessage langBreakLootBlockPrevented;

    public LootChestProtector(Context<Core> context) {
        super(context);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBreakLootChest(final BlockBreakEvent event) {
        if (!configWarnBreakingLootBlocks) {
            return;
        }

        final var state = event.getBlock().getState(false);
        if (!(state instanceof Lootable)) {
            return;
        }

        final var lootable = (Lootable) state;
        if (!lootable.hasLootTable()) {
            return;
        }

        final var block = event.getBlock();
        final var player = event.getPlayer();
        var blockAttempts = lootBreakAttempts.get(block);
        final var now = System.currentTimeMillis();
        if (blockAttempts != null) {
            final var playerAttemptTime = blockAttempts.get(player.getUniqueId());
            if (playerAttemptTime != null) {
                final var elapsed = now - playerAttemptTime;
                if (elapsed > 5000 && elapsed < 30000) {
                    // Allow
                    return;
                }
            } else {
                blockAttempts.put(player.getUniqueId(), now);
            }
        } else {
            blockAttempts = new HashMap<UUID, Long>();
            blockAttempts.put(player.getUniqueId(), now);
            lootBreakAttempts.put(block, blockAttempts);
        }

        langBreakLootBlockPrevented.send(player);
        event.setCancelled(true);
    }
}
