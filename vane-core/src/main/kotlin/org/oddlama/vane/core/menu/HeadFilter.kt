package org.oddlama.vane.core.menu

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.core.material.HeadMaterial
import org.oddlama.vane.core.module.Context

class HeadFilter : Filter<HeadMaterial> {
    private var str: String? = null

    override fun openFilterSettings(
        context: Context<*>,
        player: Player,
        filterTitle: String,
        returnTo: Menu?
    ) {
        MenuFactory.anvilStringInput(
            context,
            player,
            filterTitle,
            ItemStack(Material.PAPER),
            "?"
        ) { t1, t2, t3 ->
            t2!!.close(t1!!)
            str = t3?.lowercase()
            returnTo?.open(t1)
            Menu.ClickResult.SUCCESS
        }.open(player)
    }

    override fun reset() {
        str = null
    }

    private fun filterByCategory(material: HeadMaterial): Boolean =
        material.category?.lowercase()?.contains(str ?: return false) == true

    private fun filterByTags(material: HeadMaterial): Boolean {
        val filter = str ?: return false
        return material.tags.any { it?.lowercase()?.contains(filter) == true }
    }

    private fun filterByName(material: HeadMaterial): Boolean =
        material.name?.lowercase()?.contains(str ?: return false) == true

    override fun filter(things: MutableList<HeadMaterial?>): MutableList<HeadMaterial?> {
        if (str == null) return things
        return things.filterTo(mutableListOf()) { t ->
            t != null && (filterByCategory(t) || filterByTags(t) || filterByName(t))
        }
    }
}
