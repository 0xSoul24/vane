package org.oddlama.vane.portals.menu

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
import org.oddlama.vane.portals.Portals

class EnterNameMenu(context: Context<Portals?>) : ModuleComponent<Portals?>(context.namespace("EnterName")) {
    @LangMessage
    var langTitle: TranslatedMessage? = null

    @ConfigMaterial(def = Material.ENDER_PEARL, desc = "The item used to name portals.")
    var configMaterial: Material? = null

    fun create(player: Player, onClick: Function2<Player?, String?, Menu.ClickResult?>): Menu {
        return create(player, "Name", onClick)
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
