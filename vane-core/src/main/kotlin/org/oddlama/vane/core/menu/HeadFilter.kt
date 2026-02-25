package org.oddlama.vane.core.menu

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.core.material.HeadMaterial
import org.oddlama.vane.core.module.Context
import java.util.*
import java.util.stream.Collectors

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
            str = t3?.lowercase(Locale.getDefault())
            returnTo?.open(t1)
            Menu.ClickResult.SUCCESS
        }.open(player)
    }

    override fun reset() {
        str = null
    }

    private fun filterByCategories(material: HeadMaterial): Boolean {
        val cat = material.category() ?: return false
        return cat.lowercase(Locale.getDefault()).contains(str ?: return false)
    }

    private fun filterByTags(material: HeadMaterial): Boolean {
        for (tag in material.tags()) {
            if (tag != null && tag.lowercase(Locale.getDefault()).contains(str ?: return false)) {
                return true
            }
        }

        return false
    }

    private fun filterByName(material: HeadMaterial): Boolean {
        val name = material.name() ?: return false
        return name.lowercase(Locale.getDefault()).contains(str ?: return false)
    }

    override fun filter(things: MutableList<HeadMaterial?>?): MutableList<HeadMaterial?>? {
        if (things == null) return null

        if (str == null) {
            return things
        }

        return things
            .stream()
            .filter { t: HeadMaterial? -> t != null && (filterByCategories(t) || filterByTags(t) || filterByName(t)) }
            .collect(Collectors.toList())
    }
}
