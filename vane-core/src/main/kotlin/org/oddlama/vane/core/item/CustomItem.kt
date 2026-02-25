package org.oddlama.vane.core.item

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.annotation.config.ConfigInt
import org.oddlama.vane.annotation.item.VaneItem
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.config.loot.LootTableList
import org.oddlama.vane.core.config.loot.LootTables
import org.oddlama.vane.core.config.recipes.RecipeList
import org.oddlama.vane.core.config.recipes.Recipes
import org.oddlama.vane.core.item.api.CustomItem
import org.oddlama.vane.core.item.api.InhibitBehavior
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.util.StorageUtil.namespacedKey
import org.oddlama.vane.util.snakeCaseToPascalCase
import java.util.EnumSet

open class CustomItem<T : Module<T?>?> @JvmOverloads constructor(
    context: Context<T?>,
    nameOverride: String? = null,
    customModelDataOverride: Int? = null
) : Listener<T?>(null), CustomItem {
    private var annotation: VaneItem? = null
    var key: NamespacedKey

    var recipes: Recipes<T?>?
    var lootTables: LootTables<T?>?

    // Language
    @LangMessage
    var langName: TranslatedMessage? = null

    @ConfigInt(
        def = 0,
        min = 0,
        desc = "The durability of this item. Set to 0 to use the durability properties of whatever base material the item is made of."
    )
    private var configDurability = 0

    private val nameOverride: String?
    private val customModelDataOverride: Int?

    init {
        var context = context
        var cls: Class<*>? = javaClass
        while (this.annotation == null && cls != null) {
            this.annotation = cls.getAnnotation(VaneItem::class.java)
            cls = cls.getSuperclass()
        }
        checkNotNull(this.annotation) { "Could not find @VaneItem annotation on $javaClass" }

        this.nameOverride = nameOverride
        this.customModelDataOverride = customModelDataOverride

        // Set namespace delayed, as we need to access instance methods to do so.
        context = context.group("Item" + snakeCaseToPascalCase(name()), "Enable item " + name())
        setContext(context)

        this.key = namespacedKey(module!!.namespace(), name())
        recipes = Recipes(getContext(), this.key, { this.defaultRecipes() })
        lootTables = LootTables(getContext(), this.key, { this.defaultLootTables() })

        // Register item (use safe-call in case the item registry isn't initialized yet)
        module!!.core?.itemRegistry()?.register(this)
    }

    override fun key(): NamespacedKey {
        return key
    }

    fun name(): String {
        if (nameOverride != null) {
            return nameOverride
        }
        return annotation!!.name
    }

    override fun enabled(): Boolean {
        // Explicitly stated to not be forgotten, as enabled() is also part of
        // Listener<T>.
        return annotation!!.enabled && super.enabled()
    }

    override fun version(): Int {
        return annotation!!.version
    }

    override fun baseMaterial(): Material? {
        return annotation!!.base
    }

    override fun customModelData(): Int {
        if (customModelDataOverride != null) {
            return customModelDataOverride
        }
        return annotation!!.modelData
    }

    override fun displayName(): Component? {
        return langName!!.format().decoration(TextDecoration.ITALIC, false)
    }

    fun configDurabilityDef(): Int {
        return annotation!!.durability
    }

    override fun durability(): Int {
        return configDurability
    }

    override fun updateItemStack(itemStack: ItemStack): ItemStack {
        return itemStack
    }

    override fun inhibitedBehaviors(): EnumSet<InhibitBehavior> {
        return EnumSet.of(InhibitBehavior.USE_IN_VANILLA_RECIPE)
    }

    open fun defaultRecipes(): RecipeList? {
        return RecipeList.of()
    }

    open fun defaultLootTables(): LootTableList? {
        return LootTableList.of()
    }
}
