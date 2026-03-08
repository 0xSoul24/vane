package org.oddlama.vane.regions

import net.milkbowl.vault.economy.Economy
import net.milkbowl.vault.economy.EconomyResponse
import org.bukkit.OfflinePlayer
import org.bukkit.plugin.Plugin

class RegionEconomyDelegate(var module: Regions?) {
    var economy: Economy? = null

    fun setup(plugin: Plugin?): Boolean {
        if (plugin == null) {
            this.module!!.log.severe(
                "Economy was selected as the currency provider, but the Vault plugin wasn't found! Falling back to material currency."
            )
            return false
        }
        val rsp = this.module!!
            .server
            .servicesManager
            .getRegistration<Economy?>(Economy::class.java)
        if (rsp == null) {
            this.module!!.log.severe(
                "Economy was selected as the currency provider, but no Economy service provider is registered via VaultAPI! Falling back to material currency."
            )
            return false
        }

        economy = rsp.getProvider()
        return true
    }

    fun has(player: OfflinePlayer?, amount: Double): Boolean {
        return economy!!.has(player, amount)
    }

    fun withdraw(player: OfflinePlayer?, amount: Double): EconomyResponse? {
        return economy!!.withdrawPlayer(player, amount)
    }

    fun deposit(player: OfflinePlayer?, amount: Double): EconomyResponse? {
        return economy!!.depositPlayer(player, amount)
    }

    fun currencyNamePlural(): String? {
        return economy!!.currencyNamePlural()
    }

    fun fractionalDigits(): Int {
        return economy!!.fractionalDigits()
    }
}
