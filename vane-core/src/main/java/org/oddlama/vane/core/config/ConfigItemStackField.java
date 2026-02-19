package org.oddlama.vane.core.config;

import static org.oddlama.vane.util.MaterialUtil.materialFrom;
import static org.oddlama.vane.util.StorageUtil.namespacedKey;

import java.lang.reflect.Field;
import java.util.function.Function;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.annotation.config.ConfigItemStack;
import org.oddlama.vane.core.YamlLoadException;

public class ConfigItemStackField extends ConfigField<ItemStack> {

    public ConfigItemStack annotation;

    public ConfigItemStackField(
        Object owner,
        Field field,
        Function<String, String> mapName,
        ConfigItemStack annotation
    ) {
        super(owner, field, mapName, "item stack", annotation.desc());
        this.annotation = annotation;
    }

    private void appendItemStackDefinition(StringBuilder builder, String indent, String prefix, ItemStack def) {
        // Material
        builder.append(indent);
        builder.append(prefix);
        builder.append("  material: ");
        final var material =
            "\"" +
            escapeYaml(def.getType().getKey().getNamespace()) +
            ":" +
            escapeYaml(def.getType().getKey().getKey()) +
            "\"";
        builder.append(material);
        builder.append("\n");

        // Amount
        if (def.getAmount() != 1) {
            builder.append(indent);
            builder.append(prefix);
            builder.append("  amount: ");
            builder.append(def.getAmount());
            builder.append("\n");
        }
    }

    @Override
    public ItemStack def() {
        final var override = overriddenDef();
        if (override != null) {
            return override;
        } else {
            return new ItemStack(annotation.def().type(), annotation.def().amount());
        }
    }

    @Override
    public boolean metrics() {
        final var override = overriddenMetrics();
        if (override != null) {
            return override;
        } else {
            return annotation.metrics();
        }
    }

    @Override
    public void generateYaml(StringBuilder builder, String indent, YamlConfiguration existingCompatibleConfig) {
        appendDescription(builder, indent);

        // Default
        builder.append(indent);
        builder.append("# Default:\n");
        appendItemStackDefinition(builder, indent, "# ", def());

        // Definition
        builder.append(indent);
        builder.append(basename());
        builder.append(":\n");
        final var def = existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath())
            ? loadFromYaml(existingCompatibleConfig)
            : def();
        appendItemStackDefinition(builder, indent, "", def);
    }

    @Override
    public void checkLoadable(YamlConfiguration yaml) throws YamlLoadException {
        checkYamlPath(yaml);

        if (!yaml.isConfigurationSection(yamlPath())) {
            throw new YamlLoadException("Invalid type for yaml path '" + yamlPath() + "', expected group");
        }

        for (var varKey : yaml.getConfigurationSection(yamlPath()).getKeys(false)) {
            final var varPath = yamlPath() + "." + varKey;
            switch (varKey) {
                case "material": {
                    if (!yaml.isString(varPath)) {
                        throw new YamlLoadException("Invalid type for yaml path '" + varPath + "', expected list");
                    }

                    final var str = yaml.getString(varPath);
                    final var split = str.split(":");
                    if (split.length != 2) {
                        throw new YamlLoadException(
                            "Invalid material for yaml path '" +
                            yamlPath() +
                            "': '" +
                            str +
                            "' is not a valid namespaced key"
                        );
                    }
                    break;
                }
                case "amount": {
                    if (!(yaml.get(varPath) instanceof Number)) {
                        throw new YamlLoadException("Invalid type for yaml path '" + yamlPath() + "', expected int");
                    }
                    final var val = yaml.getInt(yamlPath());
                    if (val < 0) {
                        throw new YamlLoadException("Invalid value for yaml path '" + yamlPath() + "' Must be >= 0");
                    }
                    break;
                }
            }
        }
    }

    public ItemStack loadFromYaml(YamlConfiguration yaml) {
        var materialStr = "";
        var amount = 1;
        for (var varKey : yaml.getConfigurationSection(yamlPath()).getKeys(false)) {
            final var varPath = yamlPath() + "." + varKey;
            switch (varKey) {
                case "material": {
                    amount = 0;
                    materialStr = yaml.getString(varPath);
                    break;
                }
                case "amount": {
                    amount = yaml.getInt(varPath);
                    break;
                }
            }
        }

        final var split = materialStr.split(":");
        final var material = materialFrom(namespacedKey(split[0], split[1]));
        return new ItemStack(material, amount);
    }

    public void load(YamlConfiguration yaml) {
        try {
            field.set(owner, loadFromYaml(yaml));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
        }
    }
}
