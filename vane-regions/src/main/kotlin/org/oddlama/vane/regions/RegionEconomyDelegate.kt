package org.oddlama.vane.regions

import net.milkbowl.vault.economy.Economy
import net.milkbowl.vault.economy.EconomyResponse
import org.bukkit.OfflinePlayer
import org.bukkit.plugin.Plugin

/**
 * Thin wrapper around Vault economy APIs used by the regions module.
 */
class RegionEconomyDelegate(private val module: Regions) {
    /**
     * Economy service provider resolved from Vault.
     */
    private var economy: Economy? = null
    /**
     * Non-null accessor for the resolved economy service.
     */
    private val activeEconomy: Economy
        get() = requireNotNull(economy) { "Economy delegate used before setup() completed" }

    /**
     * Resolves and stores the active Vault economy provider.
     *
     * @return `true` when setup succeeds.
     */
    fun setup(plugin: Plugin?): Boolean {
        if (plugin == null) {
            module.log.severe(
                "Economy was selected as the currency provider, but the Vault plugin wasn't found! Falling back to material currency."
            )
            return false
        }
        val rsp = module
            .server
            .servicesManager
            .getRegistration<Economy?>(Economy::class.java)
        if (rsp == null) {
            module.log.severe(
                "Economy was selected as the currency provider, but no Economy service provider is registered via VaultAPI! Falling back to material currency."
            )
            return false
        }

        economy = rsp.provider
        return true
    }

    /**
     * Returns whether the player has at least the requested balance.
     */
    fun has(player: OfflinePlayer?, amount: Double): Boolean = activeEconomy.has(player, amount)

    /**
     * Withdraws the requested amount from a player account.
     */
    fun withdraw(player: OfflinePlayer?, amount: Double): EconomyResponse = activeEconomy.withdrawPlayer(player, amount)

    /**
     * Deposits the requested amount into a player account.
     */
    fun deposit(player: OfflinePlayer?, amount: Double): EconomyResponse = activeEconomy.depositPlayer(player, amount)

    /**
     * Returns the pluralized currency label from the active provider.
     */
    fun currencyNamePlural(): String = activeEconomy.currencyNamePlural()

    /**
     * Returns the number of supported fractional digits in this economy.
     */
    fun fractionalDigits(): Int = activeEconomy.fractionalDigits()
}
