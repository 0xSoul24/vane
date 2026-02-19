package org.oddlama.vane.core.command.params;

import java.util.Collections;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.command.check.CheckResult;
import org.oddlama.vane.core.command.check.ErrorCheckResult;
import org.oddlama.vane.core.command.check.ParseCheckResult;
import org.oddlama.vane.core.functional.Function1;

public class AnyParam<T> extends BaseParam {

    private String argumentType;
    private Function1<String, ? extends T> fromString;

    public AnyParam(Command<?> command, String argumentType, Function1<String, ? extends T> fromString) {
        super(command);
        this.argumentType = argumentType;
        this.fromString = fromString;
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
        return Collections.emptyList();
    }

    private T parse(String arg) {
        return fromString.apply(arg);
    }
}
