package org.oddlama.vane.regions.menu

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.annotation.config.ConfigMaterial
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.functional.Function2
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.menu.Menu
import org.oddlama.vane.core.menu.MenuFactory
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleComponent
import org.oddlama.vane.regions.Regions

class EnterRegionNameMenu(context: Context<Regions?>) :
    ModuleComponent<Regions?>(context.namespace("EnterRegionName")) {
    @LangMessage
    var langTitle: TranslatedMessage? = null

    @ConfigMaterial(def = Material.MAP, desc = "The item used to name regions.")
    var configMaterial: Material? = null

    fun create(player: Player, onClick: Function2<Player?, String?, Menu.ClickResult?>): Menu {
        return create(player, "Region", onClick)
    }

    fun create(
        player: Player,
        defaultName: String,
        onClick: Function2<Player?, String?, Menu.ClickResult?>
    ): Menu {
        return MenuFactory.anvilStringInput(
            getContext()!!,
            player,
            langTitle!!.str(),
            ItemStack(configMaterial!!),
            defaultName
        ) { p: Player?, menu: Menu?, name: String? ->
            menu!!.close(p!!)
            onClick.apply(p, name)
        }
    }

    public override fun onEnable() {}

    public override fun onDisable() {}
}
