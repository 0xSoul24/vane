package org.oddlama.vane.util;

import static net.kyori.adventure.text.event.HoverEvent.Action.SHOW_TEXT;
import static org.oddlama.vane.util.Nms.creativeTabId;
import static org.oddlama.vane.util.Nms.itemHandle;
import static org.oddlama.vane.util.Nms.playerHandle;

import com.destroystokyo.paper.profile.ProfileProperty;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.world.item.Item;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.material.ExtendedMaterial;

public class ItemUtil {

    private static final UUID SKULL_OWNER = UUID.fromString("00000000-0000-0000-0000-000000000000");

    public static void damageItem(final Player player, final ItemStack itemStack, final int amount) {
        if (player.getGameMode() == GameMode.CREATIVE) { // don't damage the tool if the player is in creative
            return;
        }

        if (amount <= 0) {
            return;
        }

        final var handle = itemHandle(itemStack);
        if (handle == null) {
            return;
        }

        handle.hurtAndBreak(amount, Nms.worldHandle(player.getWorld()), playerHandle(player), item -> {
            player.broadcastSlotBreak(EquipmentSlot.HAND);
            itemStack.subtract();
        });
    }

    public static String nameOf(final ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return "";
        }
        final var meta = item.getItemMeta();
        if (!meta.hasDisplayName()) {
            return "";
        }

        return PlainTextComponentSerializer.plainText().serialize(meta.displayName());
    }

    public static ItemStack nameItem(final ItemStack item, final Component name) {
        return nameItem(item, name, (List<Component>) null);
    }

    public static ItemStack nameItem(final ItemStack item, final Component name, Component lore) {
        lore = lore.decoration(TextDecoration.ITALIC, false);
        return nameItem(item, name, List.of(lore));
    }

    public static ItemStack setLore(final ItemStack item, final List<Component> lore) {
        item.editMeta(meta -> {
            final var list = lore
                .stream()
                .map(x -> x.decoration(TextDecoration.ITALIC, false))
                .collect(Collectors.toList());
            meta.lore(list);
        });

        return item;
    }

    public static ItemStack nameItem(final ItemStack item, Component name, final List<Component> lore) {
        var meta = item.getItemMeta();
		if (meta == null) {
			// Cannot name item without meta (probably air)
			return item;
		}

        name = name.decoration(TextDecoration.ITALIC, false);
        meta.displayName(name);

        if (lore != null) {
            final var list = lore
                .stream()
                .map(x -> x.decoration(TextDecoration.ITALIC, false))
                .collect(Collectors.toList());
            meta.lore(list);
        }

        item.setItemMeta(meta);
        return item;
    }

    public static int compareEnchantments(final ItemStack itemA, final ItemStack itemB) {
        var aE = itemA.getEnchantments();
        var bE = itemB.getEnchantments();

        final var aMeta = itemA.getItemMeta();
        if (aMeta instanceof EnchantmentStorageMeta) {
            final var stored = ((EnchantmentStorageMeta) aMeta).getStoredEnchants();
            if (stored.size() > 0) {
                aE = stored;
            }
        }

        final var bMeta = itemB.getItemMeta();
        if (bMeta instanceof EnchantmentStorageMeta) {
            final var stored = ((EnchantmentStorageMeta) bMeta).getStoredEnchants();
            if (stored.size() > 0) {
                bE = stored;
            }
        }

        // Unenchanted first
        final var aCount = aE.size();
        final var bCount = bE.size();
        if (aCount == 0 && bCount == 0) {
            return 0;
        } else if (aCount == 0) {
            return -1;
        } else if (bCount == 0) {
            return 1;
        }

        // More enchantments before fewer enchantments
        if (aCount != bCount) {
            return bCount - aCount;
        }

        final var aSorted = aE
            .entrySet()
            .stream()
            .sorted(
                Map.Entry.<Enchantment, Integer>comparingByKey((a, b) ->
                    a.getKey().toString().compareTo(b.getKey().toString())
                ).thenComparing(Map.Entry.comparingByValue())
            )
            .toList();
        final var bSorted = bE
            .entrySet()
            .stream()
            .sorted(
                Map.Entry.<Enchantment, Integer>comparingByKey((a, b) ->
                    a.getKey().toString().compareTo(b.getKey().toString())
                ).thenComparing(Map.Entry.comparingByValue())
            )
            .toList();

        // Lastly, compare names and levels
        final var aIt = aSorted.iterator();
        final var bIt = bSorted.iterator();

        while (aIt.hasNext()) {
            final var aEl = aIt.next();
            final var bEl = bIt.next();

            // Lexicographic name comparison
            final var nameDiff = aEl.getKey().getKey().toString().compareTo(bEl.getKey().getKey().toString());
            if (nameDiff != 0) {
                return nameDiff;
            }

            // Level
            final int levelDiff = bEl.getValue() - aEl.getValue();
            if (levelDiff != 0) {
                return levelDiff;
            }
        }

        return 0;
    }

    public static class ItemStackComparator implements Comparator<ItemStack> {

        @Override
        public int compare(final ItemStack a, final ItemStack b) {
            if (a == null && b == null) {
                return 0;
            } else if (a == null) {
                return 1;
            } else if (b == null) {
                return -1;
            }

            final var nA = itemHandle(a);
            final var nB = itemHandle(b);
            if (nA.isEmpty()) {
                return nB.isEmpty() ? 0 : 1;
            } else if (nB.isEmpty()) {
                return -1;
            }

            // By creative mode tab
            final var creativeModeTabDiff = creativeTabId(nA) - creativeTabId(nB);
            if (creativeModeTabDiff != 0) {
                return creativeModeTabDiff;
            }

            // By id
            final var idDiff = Item.getId(nA.getItem()) - Item.getId(nB.getItem());
            if (idDiff != 0) {
                return idDiff;
            }

            // By damage
            final var damageDiff = nA.getDamageValue() - nB.getDamageValue();
            if (damageDiff != 0) {
                return damageDiff;
            }

            // By count
            final var countDiff = nB.getCount() - nA.getCount();
            if (countDiff != 0) {
                return countDiff;
            }

            // By enchantments
            return compareEnchantments(a, b);
        }
    }

    public static ItemStack skullForPlayer(final OfflinePlayer player, final boolean isForMenu) {
        final var item = new ItemStack(Material.PLAYER_HEAD);
        if (!isForMenu || Core.instance().configPlayerHeadsInMenus) {
            item.editMeta(SkullMeta.class, meta -> meta.setOwningPlayer(player));
        }
        return item;
    }

    public static ItemStack skullWithTexture(final String name, final String base64Texture) {
        final var profile = Bukkit.createProfileExact(SKULL_OWNER, "-");
        profile.setProperty(new ProfileProperty("textures", base64Texture));

        final var item = new ItemStack(Material.PLAYER_HEAD);
        final var meta = (SkullMeta) item.getItemMeta();
        final var nameComponent = Component.text(name)
            .decoration(TextDecoration.ITALIC, false)
            .color(NamedTextColor.YELLOW);
        meta.displayName(nameComponent);
        meta.setPlayerProfile(profile);
        item.setItemMeta(meta);
        return item;
    }

    /** Returns true if the given component is guarded by the given sentinel. */
    public static boolean hasSentinel(final Component component, final NamespacedKey sentiel) {
        if (component == null) {
            return false;
        }

        final var hover = component.hoverEvent();
        if (hover == null) {
            return false;
        }

        if (hover.value() instanceof final TextComponent hoverText) {
            return hover.action() == SHOW_TEXT && sentiel.toString().equals(hoverText.content());
        } else {
            return false;
        }
    }

    public static Component addSentinel(final Component component, final NamespacedKey sentinel) {
        return component.hoverEvent(HoverEvent.showText(Component.text(sentinel.toString())));
    }

    /**
     * Applies enchantments to the item given in the form
     * "{<namespace:enchant>[*<level>][,<namespace:enchant>[*<level>]]...}". Throws
     * IllegalArgumentException if an enchantment cannot be found.
     */
    private static ItemStack applyEnchants(final ItemStack itemStack, @Nullable String enchants) {
        if (enchants == null) {
            return itemStack;
        }

        enchants = enchants.trim();
        if (!enchants.startsWith("{") || !enchants.endsWith("}")) {
            throw new IllegalArgumentException(
                "enchantments must be of form {<namespace:enchant>[*<level>][,<namespace:enchant>[*<level>]]...}"
            );
        }

        final var parts = enchants.substring(1, enchants.length() - 1).split(",");
        for (var part : parts) {
            part = part.trim();

            String key = part;
            int level = 1;
            final int levelDelim = key.indexOf('*');
            if (levelDelim != -1) {
                level = Integer.parseInt(key.substring(levelDelim + 1));
                key = key.substring(0, levelDelim);
            }

            final var ench = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.ENCHANTMENT)
                .get(NamespacedKey.fromString(key));
            if (ench == null) {
                throw new IllegalArgumentException(
                    "Cannot apply unknown enchantment '" + key + "' to item '" + itemStack + "'"
                );
            }

            if (itemStack.getType() == Material.ENCHANTED_BOOK) {
                final var flevel = level;
                itemStack.editMeta(EnchantmentStorageMeta.class, meta -> meta.addStoredEnchant(ench, flevel, false));
            } else {
                itemStack.addEnchantment(ench, level);
            }
        }

        if (parts.length > 0) {
            Core.instance().enchantmentManager.updateEnchantedItem(itemStack);
        }
        return itemStack;
    }

    /** Returns the itemstack and a boolean indicating whether it was just as simlpe material. */
    public static @NotNull Pair<ItemStack, Boolean> itemstackFromString(String definition) {
        // NOTE: Override to allow seamless migration from pre 1.21.9 to 1.21.9+
        if ("minecraft:chain".equalsIgnoreCase(definition)) {
            definition = "minecraft:iron_chain";
        }

        // namespace:key[[components]][#enchants{}], where the key can reference a
        // material, head material or customitem.
        final var enchantsDelim = definition.indexOf("#enchants{");
        String enchants = null;
        if (enchantsDelim != -1) {
            enchants = definition.substring(enchantsDelim + 9); // Let it start at '{'
            definition = definition.substring(0, enchantsDelim);
        }

        final var nbtDelim = definition.indexOf('[');
        NamespacedKey key;
        if (nbtDelim == -1) {
            key = NamespacedKey.fromString(definition);
        } else {
            key = NamespacedKey.fromString(definition.substring(0, nbtDelim));
        }

        final var emat = ExtendedMaterial.from(key);
        if (emat == null) {
            throw new IllegalArgumentException("Invalid extended material definition: " + definition);
        }

        // First, create the itemstack as if we had no NBT information.
        final var itemStack = emat.item();

        // If there is no NBT information, we can return here.
        if (nbtDelim == -1) {
            return Pair.of(applyEnchants(itemStack, enchants), emat.isSimpleMaterial() && enchants == null);
        }

        // Parse the NBT by using minecraft's internal parser with the base material
        // of whatever the extended material gave us.
        final var vanillaDefinition = itemStack.getType().key() + definition.substring(nbtDelim);
        try {
            final var parsedNbt = new ItemParser(Commands.createValidationContext(VanillaRegistries.createLookup()))
                .parse(new StringReader(vanillaDefinition))
                .components();

            // Now apply the NBT be parsed by minecraft's internal parser to the itemstack.
            final var nmsItem = itemHandle(itemStack).copy();
            nmsItem.applyComponents(parsedNbt);

            return Pair.of(applyEnchants(CraftItemStack.asCraftMirror(nmsItem), enchants), false);
        } catch (final CommandSyntaxException e) {
            throw new IllegalArgumentException("Could not parse NBT of item definition: " + definition, e);
        }
    }
}