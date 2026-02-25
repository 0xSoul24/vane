package org.oddlama.vane.core.menu

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.core.functional.Function2
import org.oddlama.vane.core.module.Context
import java.util.*
import java.util.stream.Collectors

interface Filter<T> {
    fun openFilterSettings(
        context: Context<*>,
        player: Player,
        filterTitle: String,
        returnTo: Menu?
    )

    fun reset()

    fun filter(things: MutableList<T?>?): MutableList<T?>?

    class StringFilter<T> @JvmOverloads constructor(
        private val doFilter: Function2<T?, String?, Boolean?>,
        private val ignoreCase: Boolean = true
    ) : Filter<T> {
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
                str = t3
                returnTo?.open(t1)
                Menu.ClickResult.SUCCESS
            }.open(player)
        }

        override fun reset() {
            str = null
        }

        override fun filter(things: MutableList<T?>?): MutableList<T?>? {
            if (things == null) return null

            if (str == null) {
                return things
            } else {
                val fStr: String? = if (ignoreCase) {
                    str!!.lowercase(Locale.getDefault())
                } else {
                    str
                }

                return things.stream().filter { t: T? -> doFilter.apply(t, fStr)!! }.collect(Collectors.toList())
            }
        }
    }
}
