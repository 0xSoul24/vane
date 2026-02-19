package org.oddlama.vane.core.command;

import static org.oddlama.vane.util.StorageUtil.namespacedKey;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.oddlama.vane.core.command.check.CheckResult;
import org.oddlama.vane.core.command.check.CombinedErrorCheckResult;
import org.oddlama.vane.core.command.check.ErrorCheckResult;
import org.oddlama.vane.core.command.check.ParseCheckResult;
import org.oddlama.vane.core.command.params.AnyParam;
import org.oddlama.vane.core.command.params.ChoiceParam;
import org.oddlama.vane.core.command.params.DynamicChoiceParam;
import org.oddlama.vane.core.command.params.FixedParam;
import org.oddlama.vane.core.command.params.SentinelExecutorParam;
import org.oddlama.vane.core.functional.Consumer1;
import org.oddlama.vane.core.functional.Consumer2;
import org.oddlama.vane.core.functional.Consumer3;
import org.oddlama.vane.core.functional.Consumer4;
import org.oddlama.vane.core.functional.Consumer5;
import org.oddlama.vane.core.functional.Consumer6;
import org.oddlama.vane.core.functional.Function1;
import org.oddlama.vane.core.functional.Function2;
import org.oddlama.vane.core.functional.Function3;
import org.oddlama.vane.core.functional.Function4;
import org.oddlama.vane.core.functional.Function5;
import org.oddlama.vane.core.functional.Function6;
import org.oddlama.vane.core.module.Module;

@SuppressWarnings("overloads")
public interface Param {
    public List<Param> getParams();

    public default boolean requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            getCommand().getModule().core.langCommandNotAPlayer.send(sender);
            return false;
        }

        return true;
    }

    public default boolean isExecutor() {
        return false;
    }

    public default void addParam(Param param) {
        if (param.isExecutor() && getParams().stream().anyMatch(p -> p.isExecutor())) {
            throw new RuntimeException("Cannot define multiple executors for the same parameter! This is a bug.");
        }
        getParams().add(param);
    }

    public default <T1> void execPlayer(Consumer1<T1> f) {
        addParam(new SentinelExecutorParam<>(getCommand(), f, this::requirePlayer, i -> i == 0));
    }

    public default <T1, T2> void execPlayer(Consumer2<T1, T2> f) {
        addParam(new SentinelExecutorParam<>(getCommand(), f, this::requirePlayer, i -> i == 0));
    }

    public default <T1, T2, T3> void execPlayer(Consumer3<T1, T2, T3> f) {
        addParam(new SentinelExecutorParam<>(getCommand(), f, this::requirePlayer, i -> i == 0));
    }

    public default <T1, T2, T3, T4> void execPlayer(Consumer4<T1, T2, T3, T4> f) {
        addParam(new SentinelExecutorParam<>(getCommand(), f, this::requirePlayer, i -> i == 0));
    }

    public default <T1, T2, T3, T4, T5> void execPlayer(Consumer5<T1, T2, T3, T4, T5> f) {
        addParam(new SentinelExecutorParam<>(getCommand(), f, this::requirePlayer, i -> i == 0));
    }

    public default <T1, T2, T3, T4, T5, T6> void execPlayer(Consumer6<T1, T2, T3, T4, T5, T6> f) {
        addParam(new SentinelExecutorParam<>(getCommand(), f, this::requirePlayer, i -> i == 0));
    }

    public default <T1> void execPlayer(Function1<T1, Boolean> f) {
        addParam(new SentinelExecutorParam<>(getCommand(), f, this::requirePlayer, i -> i == 0));
    }

    public default <T1, T2> void execPlayer(Function2<T1, T2, Boolean> f) {
        addParam(new SentinelExecutorParam<>(getCommand(), f, this::requirePlayer, i -> i == 0));
    }

    public default <T1, T2, T3> void execPlayer(Function3<T1, T2, T3, Boolean> f) {
        addParam(new SentinelExecutorParam<>(getCommand(), f, this::requirePlayer, i -> i == 0));
    }

    public default <T1, T2, T3, T4> void execPlayer(Function4<T1, T2, T3, T4, Boolean> f) {
        addParam(new SentinelExecutorParam<>(getCommand(), f, this::requirePlayer, i -> i == 0));
    }

    public default <T1, T2, T3, T4, T5> void execPlayer(Function5<T1, T2, T3, T4, T5, Boolean> f) {
        addParam(new SentinelExecutorParam<>(getCommand(), f, this::requirePlayer, i -> i == 0));
    }

    public default <T1, T2, T3, T4, T5, T6> void execPlayer(Function6<T1, T2, T3, T4, T5, T6, Boolean> f) {
        addParam(new SentinelExecutorParam<>(getCommand(), f, this::requirePlayer, i -> i == 0));
    }

    public default <T1> void exec(Consumer1<T1> f) {
        addParam(new SentinelExecutorParam<>(getCommand(), f));
    }

    public default <T1, T2> void exec(Consumer2<T1, T2> f) {
        addParam(new SentinelExecutorParam<>(getCommand(), f));
    }

    public default <T1, T2, T3> void exec(Consumer3<T1, T2, T3> f) {
        addParam(new SentinelExecutorParam<>(getCommand(), f));
    }

    public default <T1, T2, T3, T4> void exec(Consumer4<T1, T2, T3, T4> f) {
        addParam(new SentinelExecutorParam<>(getCommand(), f));
    }

    public default <T1, T2, T3, T4, T5> void exec(Consumer5<T1, T2, T3, T4, T5> f) {
        addParam(new SentinelExecutorParam<>(getCommand(), f));
    }

    public default <T1, T2, T3, T4, T5, T6> void exec(Consumer6<T1, T2, T3, T4, T5, T6> f) {
        addParam(new SentinelExecutorParam<>(getCommand(), f));
    }

    public default <T1> void exec(Function1<T1, Boolean> f) {
        addParam(new SentinelExecutorParam<>(getCommand(), f));
    }

    public default <T1, T2> void exec(Function2<T1, T2, Boolean> f) {
        addParam(new SentinelExecutorParam<>(getCommand(), f));
    }

    public default <T1, T2, T3> void exec(Function3<T1, T2, T3, Boolean> f) {
        addParam(new SentinelExecutorParam<>(getCommand(), f));
    }

    public default <T1, T2, T3, T4> void exec(Function4<T1, T2, T3, T4, Boolean> f) {
        addParam(new SentinelExecutorParam<>(getCommand(), f));
    }

    public default <T1, T2, T3, T4, T5> void exec(Function5<T1, T2, T3, T4, T5, Boolean> f) {
        addParam(new SentinelExecutorParam<>(getCommand(), f));
    }

    public default <T1, T2, T3, T4, T5, T6> void exec(Function6<T1, T2, T3, T4, T5, T6, Boolean> f) {
        addParam(new SentinelExecutorParam<>(getCommand(), f));
    }

    public default Param anyString() {
        return any("string", str -> str);
    }

    public default <T> AnyParam<? extends T> any(String argumentType, Function1<String, ? extends T> fromString) {
        var p = new AnyParam<>(getCommand(), argumentType, fromString);
        addParam(p);
        return p;
    }

    public default FixedParam<String> fixed(String fixed) {
        return fixed(fixed, str -> str);
    }

    public default <T> FixedParam<T> fixed(T fixed, Function1<T, String> toString) {
        var p = new FixedParam<>(getCommand(), fixed, toString);
        addParam(p);
        return p;
    }

    public default Param choice(Collection<String> choices) {
        return choice("choice", choices, str -> str);
    }

    public default <T> ChoiceParam<T> choice(
        String argumentType,
        Collection<? extends T> choices,
        Function1<T, String> toString
    ) {
        var p = new ChoiceParam<>(getCommand(), argumentType, choices, toString);
        addParam(p);
        return p;
    }

    public default <T> DynamicChoiceParam<T> choice(
        String argumentType,
        Function1<CommandSender, Collection<? extends T>> choices,
        Function2<CommandSender, T, String> toString,
        Function2<CommandSender, String, ? extends T> fromString
    ) {
        var p = new DynamicChoiceParam<>(getCommand(), argumentType, choices, toString, fromString);
        addParam(p);
        return p;
    }

    public default DynamicChoiceParam<Module<?>> chooseModule() {
        return choice(
            "module",
            sender -> getCommand().getModule().core.getModules(),
            (sender, m) -> m.getAnnotationName(),
            (sender, str) ->
                getCommand()
                    .getModule()
                    .core.getModules()
                    .stream()
                    .filter(k -> k.getAnnotationName().equalsIgnoreCase(str))
                    .findFirst()
                    .orElse(null)
        );
    }

    public default DynamicChoiceParam<World> chooseWorld() {
        return choice(
            "world",
            sender -> getCommand().getModule().getServer().getWorlds(),
            (sender, w) -> w.getName().toLowerCase(),
            (sender, str) ->
                getCommand()
                    .getModule()
                    .getServer()
                    .getWorlds()
                    .stream()
                    .filter(w -> w.getName().equalsIgnoreCase(str))
                    .findFirst()
                    .orElse(null)
        );
    }

    public default DynamicChoiceParam<OfflinePlayer> chooseAnyPlayer() {
        return choice(
            "any_player",
            sender -> getCommand().getModule().getOfflinePlayersWithValidName(),
            (sender, p) -> p.getName(),
            (sender, str) ->
                getCommand()
                    .getModule()
                    .getOfflinePlayersWithValidName()
                    .stream()
                    .filter(k -> k.getName().equalsIgnoreCase(str))
                    .findFirst()
                    .orElse(null)
        );
    }

    public default DynamicChoiceParam<Player> chooseOnlinePlayer() {
        return choice(
            "online_player",
            sender -> getCommand().getModule().getServer().getOnlinePlayers(),
            (sender, p) -> p.getName(),
            (sender, str) ->
                getCommand()
                    .getModule()
                    .getServer()
                    .getOnlinePlayers()
                    .stream()
                    .filter(k -> k.getName().equalsIgnoreCase(str))
                    .findFirst()
                    .orElse(null)
        );
    }

    // TODO (minor): Make choosePermission filter results based on the previously
    // specified player.
    public default DynamicChoiceParam<Permission> choosePermission() {
        return choice(
            "permission",
            sender -> getCommand().getModule().getServer().getPluginManager().getPermissions(),
            (sender, p) -> p.getName(),
            (sender, str) -> getCommand().getModule().getServer().getPluginManager().getPermission(str)
        );
    }

    public default ChoiceParam<GameMode> chooseGamemode() {
        return choice("gamemode", List.of(GameMode.values()), m -> m.name().toLowerCase()).ignoreCase();
    }

    public default DynamicChoiceParam<Enchantment> chooseEnchantment() {
        return chooseEnchantment((sender, e) -> true);
    }

    public default DynamicChoiceParam<Enchantment> chooseEnchantment(
        final Function2<CommandSender, Enchantment, Boolean> filter
    ) {
        return choice(
            "enchantment",
            sender ->
                RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.ENCHANTMENT)
                    .stream()
                    .filter(e -> filter.apply(sender, e))
                    .collect(Collectors.toList()),
            (sender, e) -> e.getKey().toString(),
            (sender, str) -> {
                var parts = str.split(":");
                if (parts.length != 2) {
                    return null;
                }
                var e = RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.ENCHANTMENT)
                    .get(namespacedKey(parts[0], parts[1]));
                if (!filter.apply(sender, e)) {
                    return null;
                }
                return e;
            }
        );
    }

    public default CheckResult checkAcceptDelegate(CommandSender sender, String[] args, int offset) {
        if (getParams().isEmpty()) {
            throw new RuntimeException("Encountered parameter without sentinel! This is a bug.");
        }

        // Delegate to children
        var results = getParams().stream().map(p -> p.checkAccept(sender, args, offset + 1)).toList();

        // Return the first executor result, if any
        for (var r : results) {
            if (r.good()) {
                return r;
            }
        }

        // Only retain errors from maximum depth
        var maxDepth = results.stream().map(r -> r.depth()).reduce(0, Integer::max);

        var errors = results
            .stream()
            .filter(r -> r.depth() == maxDepth)
            .map(ErrorCheckResult.class::cast)
            .collect(Collectors.toList());

        // If there is only a single max-depth sub-error, propagate it.
        // Otherwise, combine multiple errors into new error.
        if (errors.size() == 1) {
            return errors.get(0);
        } else {
            return new CombinedErrorCheckResult(errors);
        }
    }

    public default CheckResult checkAccept(CommandSender sender, String[] args, int offset) {
        var result = checkParse(sender, args, offset);
        if (!(result instanceof ParseCheckResult)) {
            return result;
        }

        var p = (ParseCheckResult) result;
        return checkAcceptDelegate(sender, args, offset).prepend(p.argumentType(), p.parsed(), p.includeParam());
    }

    public default List<String> buildCompletionsDelegate(CommandSender sender, String[] args, int offset) {
        if (getParams().isEmpty()) {
            throw new RuntimeException("Encountered parameter without sentinel! This is a bug.");
        }

        // Delegate to children
        return getParams()
            .stream()
            .map(p -> p.buildCompletions(sender, args, offset + 1))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    public default List<String> buildCompletions(CommandSender sender, String[] args, int offset) {
        if (offset < args.length - 1) {
            // We are not the last argument.
            // Delegate completion to children if the param accepts the given arguments,
            // return no completions if it doesn't
            if (checkParse(sender, args, offset) instanceof ParseCheckResult) {
                return buildCompletionsDelegate(sender, args, offset);
            } else {
                return Collections.emptyList();
            }
        } else {
            // We are the parameter that needs to be completed.
            // Offer (partial) completions if our depth is the completion depth
            return completionsFor(sender, args, offset);
        }
    }

    public List<String> completionsFor(CommandSender sender, String[] args, int offset);

    public CheckResult checkParse(CommandSender sender, String[] args, int offset);

    public Command<?> getCommand();
}
