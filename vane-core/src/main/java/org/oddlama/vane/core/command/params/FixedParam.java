package org.oddlama.vane.core.command.params;

import java.util.Collections;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.command.check.CheckResult;
import org.oddlama.vane.core.command.check.ErrorCheckResult;
import org.oddlama.vane.core.command.check.ParseCheckResult;
import org.oddlama.vane.core.functional.Function1;

public class FixedParam<T> extends BaseParam {

    private T fixedArg;
    private String fixedArgStr;
    private boolean includeParam = false;
    private boolean ignoreCase = false;

    public FixedParam(Command<?> command, T fixedArg, Function1<T, String> toString) {
        super(command);
        this.fixedArg = fixedArg;
        this.fixedArgStr = toString.apply(fixedArg);
    }

    /** Will ignore the case of the given argument when matching */
    public FixedParam<T> ignoreCase() {
        this.ignoreCase = true;
        return this;
    }

    /** Will pass this fixed parameter as an argument to the executed function */
    public FixedParam<T> includeParam() {
        this.includeParam = true;
        return this;
    }

    @Override
    public CheckResult checkParse(CommandSender sender, String[] args, int offset) {
        if (args.length <= offset) {
            return new ErrorCheckResult(offset, "§6missing argument: §3" + fixedArgStr + "§r");
        }
        var parsed = parse(args[offset]);
        if (parsed == null) {
            return new ErrorCheckResult(
                offset,
                "§6invalid argument: expected §3" + fixedArgStr + "§6 got §b" + args[offset] + "§r"
            );
        }
        return new ParseCheckResult(offset, fixedArgStr, parsed, includeParam);
    }

    @Override
    public List<String> completionsFor(CommandSender sender, String[] args, int offset) {
        return Collections.singletonList(fixedArgStr);
    }

    private T parse(String arg) {
        if (ignoreCase) {
            if (arg.equalsIgnoreCase(fixedArgStr)) {
                return fixedArg;
            }
        } else {
            if (arg.equals(fixedArgStr)) {
                return fixedArg;
            }
        }

        return null;
    }
}
