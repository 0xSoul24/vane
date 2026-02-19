package org.oddlama.vane.core.menu;

import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.core.functional.Function2;
import org.oddlama.vane.core.menu.Menu.ClickResult;
import org.oddlama.vane.core.module.Context;

public interface Filter<T> {
    public void openFilterSettings(
        final Context<?> context,
        final Player player,
        final String filterTitle,
        final Menu returnTo
    );

    public void reset();

    public List<T> filter(final List<T> things);

    public static class StringFilter<T> implements Filter<T> {

        private String str = null;
        private Function2<T, String, Boolean> doFilter;
        private boolean ignoreCase;

        public StringFilter(final Function2<T, String, Boolean> doFilter) {
            this(doFilter, true);
        }

        public StringFilter(final Function2<T, String, Boolean> doFilter, boolean ignoreCase) {
            this.doFilter = doFilter;
            this.ignoreCase = ignoreCase;
        }

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
                    str = s;
                    returnTo.open(p);
                    return ClickResult.SUCCESS;
                }
            ).open(player);
        }

        @Override
        public void reset() {
            str = null;
        }

        @Override
        public List<T> filter(final List<T> things) {
            if (str == null) {
                return things;
            } else {
                final String fStr;
                if (ignoreCase) {
                    fStr = str.toLowerCase();
                } else {
                    fStr = str;
                }

                return things.stream().filter(t -> doFilter.apply(t, fStr)).collect(Collectors.toList());
            }
        }
    }
}
