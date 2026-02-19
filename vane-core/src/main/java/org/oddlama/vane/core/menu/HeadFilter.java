package org.oddlama.vane.core.menu;

import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.core.material.HeadMaterial;
import org.oddlama.vane.core.menu.Menu.ClickResult;
import org.oddlama.vane.core.module.Context;

public class HeadFilter implements Filter<HeadMaterial> {

    private String str = null;

    public HeadFilter() {}

    @Override
    public void openFilterSettings(
        final Context<?> context,
        final Player player,
        final String filterTitle,
        final Menu returnTo
    ) {
        MenuFactory.anvilStringInput(
            context,
            player,
                filterTitle,
            new ItemStack(Material.PAPER),
            "?",
            (p, menu, s) -> {
                menu.close(p);
                str = s.toLowerCase();
                returnTo.open(p);
                return ClickResult.SUCCESS;
            }
        ).open(player);
    }

    @Override
    public void reset() {
        str = null;
    }

    private boolean filterByCategories(final HeadMaterial material) {
        return material.category().toLowerCase().contains(str);
    }

    private boolean filterByTags(final HeadMaterial material) {
        for (final var tag : material.tags()) {
            if (tag.toLowerCase().contains(str)) {
                return true;
            }
        }

        return false;
    }

    private boolean filterByName(final HeadMaterial material) {
        return material.name().toLowerCase().contains(str);
    }

    @Override
    public List<HeadMaterial> filter(final List<HeadMaterial> things) {
        if (str == null) {
            return things;
        }

        return things
            .stream()
            .filter(t -> filterByCategories(t) || filterByTags(t) || filterByName(t))
            .collect(Collectors.toList());
    }
}
