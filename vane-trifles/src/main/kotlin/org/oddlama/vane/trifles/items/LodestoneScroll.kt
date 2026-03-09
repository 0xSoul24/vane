package org.oddlama.vane.trifles.items

import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.oddlama.vane.annotation.item.VaneItem
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.config.recipes.RecipeList
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition
import org.oddlama.vane.core.item.api.CustomItem
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.trifles.Trifles
import org.oddlama.vane.util.PlayerUtil
import org.oddlama.vane.util.StorageUtil
import java.util.function.Consumer

@VaneItem(
    name = "lodestone_scroll",
    base = Material.WARPED_FUNGUS_ON_A_STICK,
    durability = 15,
    modelData = 0x760011,
    version = 1
)
class LodestoneScroll(context: Context<Trifles?>) : Scroll(context, 6000) {
    @LangMessage
    var langTeleportNoBoundLodestone: TranslatedMessage? = null

    @LangMessage
    var langTeleportMissingLodestone: TranslatedMessage? = null

    @LangMessage
    var langBoundLore: TranslatedMessage? = null

    override fun defaultRecipes(): RecipeList {
        return RecipeList.of(
            ShapedRecipeDefinition("generic")
                .shape("ABA", "EPE")
                .setIngredient('P', "vane_trifles:papyrus_scroll")
                .setIngredient('E', Material.ENDER_PEARL)
                .setIngredient('A', Material.AMETHYST_SHARD)
                .setIngredient('B', Material.NETHERITE_INGOT)
                .result(key().toString())
        )
    }

    private fun getLodestoneLocation(scroll: ItemStack?): Location? {
        if (scroll == null) {
            return null
        }
        val meta = scroll.itemMeta ?: return null
        return StorageUtil.storageGetLocation(
            meta.persistentDataContainer,
            LODESTONE_LOCATION,
            null
        )
    }

    override fun teleportLocation(scroll: ItemStack?, player: Player?, imminentTeleport: Boolean): Location? {
        val p = player ?: return null
        // This scroll cannot be used while sneaking to allow re-binding
        if (p.isSneaking) {
            return null
        }

        val lodestoneLocation = getLodestoneLocation(scroll)
        var lodestone = lodestoneLocation?.block

        if (imminentTeleport) {
            if (lodestoneLocation == null) {
                langTeleportNoBoundLodestone!!.sendActionBar(p)
            } else if (lodestone?.type != Material.LODESTONE) {
                langTeleportMissingLodestone!!.sendActionBar(p)
                lodestone = null
            }
        }

        return lodestone?.location?.add(0.5, 1.005, 0.5)
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        // Skip if no block clicked or the item is allowed to be used (e.g., torches in offhand)
        if (!event.hasBlock() || event.action != Action.RIGHT_CLICK_BLOCK || event.useItemInHand() == Event.Result.ALLOW
        ) {
            return
        }

        val block = event.clickedBlock
        if (block?.type != Material.LODESTONE) {
            return
        }

        // Only if player sneak-right-clicks
        val player = event.player
        if (!player.isSneaking) {
            return
        }

        // With a lodestone scroll in the main hand
        val item = player.equipment.getItem(EquipmentSlot.HAND)
        val customItem: CustomItem? = module!!.core?.itemRegistry()?.get(item)
        if (customItem !is LodestoneScroll || !customItem.enabled()) {
            return
        }

        // Save lodestone location
        item.editMeta(Consumer { meta: ItemMeta? ->
            StorageUtil.storageSetLocation(
                meta!!.persistentDataContainer,
                LODESTONE_LOCATION,
                block.location.add(0.5, 0.5, 0.5)
            )
            meta.lore(
                listOf(
                    langBoundLore!!
                        .format(
                            "§a" + block.world.name,
                            "§b" + block.x,
                            "§b" + block.y,
                            "§b" + block.z
                        )
                        .decoration(TextDecoration.ITALIC, false)
                )
            )
        })

        // Effects and sound
        PlayerUtil.swingArm(player, event.hand!!)
        block
            .world
            .spawnParticle(Particle.ENCHANT, block.location.add(0.5, 2.0, 0.5), 100, 0.1, 0.3, 0.1, 2.0)
        block
            .world
            .playSound(block.location, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.BLOCKS, 1.0f, 3.0f)

        // Prevent offhand from triggering (e.g., placing torches)
        event.setUseInteractedBlock(Event.Result.DENY)
        event.setUseItemInHand(Event.Result.DENY)
    }

    companion object {
        val LODESTONE_LOCATION: NamespacedKey = StorageUtil.namespacedKey("vane", "lodestone_location")
    }
}
