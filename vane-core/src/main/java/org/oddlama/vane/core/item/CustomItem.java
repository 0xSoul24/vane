package org.oddlama.vane.core.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.config.loot.LootTableList;
import org.oddlama.vane.core.config.loot.LootTables;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.Recipes;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.util.StorageUtil;

public class CustomItem<T extends Module<T>> extends Listener<T> implements org.oddlama.vane.core.item.api.CustomItem {

	/**
	 * Convert a snake_case string to PascalCase.
	 * For example: "empty_xp_bottle" becomes "EmptyXpBottle"
	 */
	private static String snakeCaseToPascalCase(String snakeCase) {
		StringBuilder result = new StringBuilder();
		boolean capitalizeNext = true;

		for (char c : snakeCase.toCharArray()) {
			if (c == '_') {
				capitalizeNext = true;
			} else if (capitalizeNext) {
				result.append(Character.toUpperCase(c));
				capitalizeNext = false;
			} else {
				result.append(c);
			}
		}

		return result.toString();
	}

	private VaneItem annotation;
	public NamespacedKey key;

	public Recipes<T> recipes;
	public LootTables<T> lootTables;

	// Language
	@LangMessage
	public TranslatedMessage langName;

	@ConfigInt(def = 0, min = 0, desc = "The durability of this item. Set to 0 to use the durability properties of whatever base material the item is made of.")
	private int configDurability;

	private final String nameOverride;
	private final Integer customModelDataOverride;

	public CustomItem(Context<T> context) {
		this(context, null, null);
	}

	public CustomItem(Context<T> context, String nameOverride, Integer customModelDataOverride) {
		super(null);
		Class<?> cls = getClass();
		while (this.annotation == null && cls != null) {
			this.annotation = cls.getAnnotation(VaneItem.class);
			cls = cls.getSuperclass();
		}
		if (this.annotation == null) {
			throw new IllegalStateException("Could not find @VaneItem annotation on " + getClass());
		}

		this.nameOverride = nameOverride;
		this.customModelDataOverride = customModelDataOverride;

		// Set namespace delayed, as we need to access instance methods to do so.
		context = context.group("Item" + snakeCaseToPascalCase(name()), "Enable item " + name());
		setContext(context);

		this.key = StorageUtil.namespacedKey(getModule().namespace(), name());
		recipes = new Recipes<T>(getContext(), this.key, this::defaultRecipes);
		lootTables = new LootTables<T>(getContext(), this.key, this::defaultLootTables);

		// Register item
		getModule().core.itemRegistry().register(this);
	}

	@Override
	public NamespacedKey key() {
		return key;
	}

	public String name() {
		if (nameOverride != null) {
			return nameOverride;
		}
		return annotation.name();
	}

	@Override
	public boolean enabled() {
		// Explicitly stated to not be forgotten, as enabled() is also part of
		// Listener<T>.
		return annotation.enabled() && super.enabled();
	}

	@Override
	public int version() {
		return annotation.version();
	}

	@Override
	public Material baseMaterial() {
		return annotation.base();
	}

	@Override
	public int customModelData() {
		if (customModelDataOverride != null) {
			return customModelDataOverride;
		}
		return annotation.modelData();
	}

	@Override
	public Component displayName() {
		return langName.format().decoration(TextDecoration.ITALIC, false);
	}

	public int configDurabilityDef() {
		return annotation.durability();
	}

	@Override
	public int durability() {
		return configDurability;
	}

	public RecipeList defaultRecipes() {
		return RecipeList.of();
	}

	public LootTableList defaultLootTables() {
		return LootTableList.of();
	}
}
