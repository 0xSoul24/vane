package org.oddlama.vane.trifles.items;

import java.util.EnumSet;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.item.api.InhibitBehavior;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;

public abstract class Scroll extends CustomItem<Trifles> {

    @ConfigInt(def = 0, min = 0, desc = "Cooldown in milliseconds until another scroll can be used.")
    public int configCooldown;

    @ConfigBoolean(def = false, desc = "Allow this scroll to be repaired via the mending enchantment.")
    private boolean configAllowMending;

    private int defaultCooldown;

    public Scroll(Context<Trifles> context, int defaultCooldown) {
        super(context);
        this.defaultCooldown = defaultCooldown;
    }

    public int configCooldownDef() {
        return defaultCooldown;
    }

    /**
     * Get the teleport location for the given player. Return null to prevent teleporting. Cooldown
     * is already handled by the base class, you only need to assert that a valid location is
     * available. For example, home scrolls may prevent teleport because of a missing bed or respawn
     * point here and notify the player about that. If imminentTeleport is true, the player will be
     * teleported if this function returns a valid location. The player should only be notified of
     * errors if this is set.
     */
    public abstract Location teleportLocation(final ItemStack scroll, final Player player, boolean imminentTeleport);

    @Override
    public EnumSet<InhibitBehavior> inhibitedBehaviors() {
        final var set = EnumSet.of(
            InhibitBehavior.USE_IN_VANILLA_RECIPE,
            InhibitBehavior.TEMPT,
            InhibitBehavior.USE_OFFHAND
        );
        if (!configAllowMending) {
            set.add(InhibitBehavior.MEND);
        }
        return set;
    }
}
