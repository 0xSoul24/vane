package org.oddlama.vane.core.config

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.annotation.config.ConfigExtendedMaterial
import org.oddlama.vane.annotation.config.ConfigInt
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.annotation.lang.LangMessageArray
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.lang.TranslatedMessageArray
import org.oddlama.vane.core.material.ExtendedMaterial
import org.oddlama.vane.core.material.ExtendedMaterial.Companion.from
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.core.module.ModuleComponent
import org.oddlama.vane.util.ItemUtil
import java.util.function.Consumer

class TranslatedItemStack<T : Module<T?>?>(
    context: Context<T?>,
    configNamespace: String?,
    private val defMaterial: ExtendedMaterial?,
    private val defAmount: Int,
    desc: String?
) : ModuleComponent<T?>(context.namespace(configNamespace, desc)) {
    @ConfigInt(def = 1, min = 0, desc = "The item stack amount.")
    var configAmount: Int = 0

    @ConfigExtendedMaterial(
        def = "minecraft:barrier",
        desc = "The item stack material. Also accepts heads from the head library or from defined custom items."
    )
    var configMaterial: ExtendedMaterial? = null

    @JvmField
    @LangMessage
    var langName: TranslatedMessage? = null

    @JvmField
    @LangMessageArray
    var langLore: TranslatedMessageArray? = null

    constructor(
        context: Context<T?>,
        configNamespace: String?,
        defMaterial: NamespacedKey,
        defAmount: Int,
        desc: String?
    ) : this(context, configNamespace, from(defMaterial), defAmount, desc)

    constructor(
        context: Context<T?>,
        configNamespace: String?,
        defMaterial: Material,
        defAmount: Int,
        desc: String?
    ) : this(context, configNamespace, from(defMaterial), defAmount, desc)

    fun item(vararg args: Any?): ItemStack {
        return ItemUtil.nameItem(
            configMaterial!!.item(configAmount)!!,
            langName!!.format(*args),
            langLore!!.format(*args)
        )
    }

    fun itemTransformLore(fLore: Consumer<MutableList<Component?>?>, vararg args: Any?): ItemStack {
        val lore = langLore!!.format(*args)
        fLore.accept(lore)
        return ItemUtil.nameItem(configMaterial!!.item(configAmount)!!, langName!!.format(*args), lore)
    }

    fun itemAmount(amount: Int, vararg args: Any?): ItemStack {
        return ItemUtil.nameItem(configMaterial!!.item(amount)!!, langName!!.format(*args), langLore!!.format(*args))
    }

    fun alternative(alternative: ItemStack, vararg args: Any?): ItemStack {
        return ItemUtil.nameItem(alternative, langName!!.format(*args), langLore!!.format(*args))
    }

    fun configMaterialDef(): ExtendedMaterial? {
        return defMaterial
    }

    fun configAmountDef(): Int {
        return defAmount
    }

    override fun onEnable() {}

    override fun onDisable() {}
}
