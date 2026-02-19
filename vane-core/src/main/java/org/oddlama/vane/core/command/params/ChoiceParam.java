package org.oddlama.vane.core.command.params;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.command.CommandSender;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.command.check.CheckResult;
import org.oddlama.vane.core.command.check.ErrorCheckResult;
import org.oddlama.vane.core.command.check.ParseCheckResult;
import org.oddlama.vane.core.functional.Function1;

public class ChoiceParam<T> extends BaseParam {

    private String argumentType;
    private Collection<? extends T> choices;
    private Function1<T, String> toString;
    private HashMap<String, T> fromString = new HashMap<>();
    private boolean ignoreCase = false;

    public ChoiceParam(
        Command<?> command,
        String argumentType,
        Collection<? extends T> choices,
        Function1<T, String> toString
    ) {
        super(command);
        this.argumentType = argumentType;
        this.choices = choices;
        this.toString = toString;
        for (var c : choices) {
            fromString.put(toString.apply(c), c);
        }
    }

    /** Will ignore the case of the given argument when matching */
    public ChoiceParam<T> ignoreCase() {
        this.ignoreCase = true;
        fromString.clear();
        for (var c : choices) {
            fromString.put(toString.apply(c), c);
        }
        return this;
    }

    @Override
    public CheckResult checkParse(CommandSender sender, String[] args, int offset) {
        if (args.length <= offset) {
            return new ErrorCheckResult(offset, "§6missing argument: §3" + argumentType + "§r");
        }
        var parsed = parse(args[offset]);
        if (parsed == null) {
            return new ErrorCheckResult(offset, "§6invalid §3" + argumentType + "§6: §b" + args[offset] + "§r");
        }
        return new ParseCheckResult(offset, argumentType, parsed, true);
    }

    @Override
    public List<String> completionsFor(CommandSender sender, String[] args, int offset) {
        return choices
            .stream()
            .map(choice -> toString.apply(choice))
            .filter(str -> str.toLowerCase().contains(args[offset].toLowerCase()))
            .collect(Collectors.toList());
    }

    private T parse(String arg) {
        if (ignoreCase) {
            return fromString.get(arg.toLowerCase());
        } else {
            return fromString.get(arg);
        }
    }
}
