package org.oddlama.vane.core.lang;

import java.util.ArrayList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.core.module.Module;

public class TranslatedMessage {

    private Module<?> module;
    private String key;
    private String defaultTranslation;

    public TranslatedMessage(final Module<?> module, final String key, final String defaultTranslation) {
        this.module = module;
        this.key = key;
        this.defaultTranslation = defaultTranslation;
    }

    public String key() {
        return key;
    }

    public String str(Object... args) {
        try {
            final var argsAsStrings = new Object[args.length];
            for (int i = 0; i < args.length; ++i) {
                if (args[i] instanceof Component) {
                    argsAsStrings[i] = LegacyComponentSerializer.legacySection().serialize((Component) args[i]);
                } else if (args[i] instanceof String) {
                    argsAsStrings[i] = args[i];
                } else {
                    throw new RuntimeException(
                        "Error while formatting message '" +
                        key() +
                        "', invalid argument to str() serializer: " +
                        args[i]
                    );
                }
            }
            return String.format(defaultTranslation, argsAsStrings);
        } catch (Exception e) {
            throw new RuntimeException("Error while formatting message '" + key() + "'", e);
        }
    }

    public @NotNull Component strComponent(Object... args) {
        return LegacyComponentSerializer.legacySection().deserialize(str(args));
    }

    public Component format(Object... args) {
        if (!module.core.configClientSideTranslations) {
            return strComponent(args);
        }

        final var list = new ArrayList<ComponentLike>();
        for (final var o : args) {
            if (o instanceof ComponentLike) {
                list.add((ComponentLike) o);
            } else if (o instanceof String) {
                list.add(LegacyComponentSerializer.legacySection().deserialize((String) o));
            } else {
                throw new RuntimeException("Error while formatting message '" + key() + "', got invalid argument " + o);
            }
        }
        return Component.translatable(key, list);
    }

    public void broadcastServerPlayers(Object... args) {
        final var component = format(args);
        for (var player : module.getServer().getOnlinePlayers()) {
            player.sendMessage(component);
        }
    }

    public void broadcastServer(Object... args) {
        final var component = format(args);
        for (var player : module.getServer().getOnlinePlayers()) {
            player.sendMessage(component);
        }
        module.clog.info(Component.text("[broadcast] ").append(strComponent(args)));
    }

    public void broadcastWorld(final World world, Object... args) {
        final var component = format(args);
        for (var player : world.getPlayers()) {
            player.sendMessage(component);
        }
    }

    public void broadcastWorldActionBar(final World world, Object... args) {
        final var component = format(args);
        for (var player : world.getPlayers()) {
            player.sendActionBar(component);
        }
    }

    public void send(final CommandSender sender, Object... args) {
        if (sender == null || sender == module.getServer().getConsoleSender()) {
            module.getServer().getConsoleSender().sendMessage(strComponent(args));
        } else {
            sender.sendMessage(format(args));
        }
    }

    public void sendActionBar(final CommandSender sender, Object... args) {
        if (sender != null && sender != module.getServer().getConsoleSender()) {
            sender.sendActionBar(format(args));
        }
    }

    public void sendAndLog(final CommandSender sender, Object... args) {
        module.clog.info(strComponent(args));

        // Also send it to sender if it's not the console
        if (sender != null && sender != module.getServer().getConsoleSender()) {
            sender.sendMessage(format(args));
        }
    }
}
