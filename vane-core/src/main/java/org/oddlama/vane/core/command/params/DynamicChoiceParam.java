package org.oddlama.vane.core.command.params;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.command.CommandSender;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.command.check.CheckResult;
import org.oddlama.vane.core.command.check.ErrorCheckResult;
import org.oddlama.vane.core.command.check.ParseCheckResult;
import org.oddlama.vane.core.functional.Function1;
import org.oddlama.vane.core.functional.Function2;

public class DynamicChoiceParam<T> extends BaseParam {

    private String argumentType;
    private Function1<CommandSender, Collection<? extends T>> choices;
    private Function2<CommandSender, T, String> toString;
    private Function2<CommandSender, String, ? extends T> fromString;

    public DynamicChoiceParam(
        Command<?> command,
        String argumentType,
        Function1<CommandSender, Collection<? extends T>> choices,
        Function2<CommandSender, T, String> toString,
        Function2<CommandSender, String, ? extends T> fromString
    ) {
        super(command);
        this.argumentType = argumentType;
        this.choices = choices;
        this.toString = toString;
        this.fromString = fromString;
    }

    @Override
    public CheckResult checkParse(CommandSender sender, String[] args, int offset) {
        if (args.length <= offset) {
            return new ErrorCheckResult(offset, "§6missing argument: §3" + argumentType + "§r");
        }
        var parsed = parse(sender, args[offset]);
        if (parsed == null) {
            return new ErrorCheckResult(offset, "§6invalid §3" + argumentType + "§6: §b" + args[offset] + "§r");
        }
        return new ParseCheckResult(offset, argumentType, parsed, true);
    }

    @Override
    public List<String> completionsFor(CommandSender sender, String[] args, int offset) {
        return choices
            .apply(sender)
            .stream()
            .map(choice -> toString.apply(sender, choice))
            .filter(str -> str.toLowerCase().contains(args[offset].toLowerCase()))
            .collect(Collectors.toList());
    }

    private T parse(CommandSender sender, String arg) {
        return fromString.apply(sender, arg);
    }
}
