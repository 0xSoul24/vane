package org.oddlama.vane.core.command.check;

import org.bukkit.command.CommandSender;
import org.oddlama.vane.core.command.Command;

public class ParseCheckResult implements CheckResult {

    private int depth;
    private String argumentType;
    private Object parsed;
    private boolean includeParam;

    public ParseCheckResult(int depth, String argumentType, Object parsed, boolean includeParam) {
        this.depth = depth;
        this.argumentType = argumentType;
        this.parsed = parsed;
        this.includeParam = includeParam;
    }

    public String argumentType() {
        return argumentType;
    }

    public Object parsed() {
        return parsed;
    }

    public boolean includeParam() {
        return includeParam;
    }

    @Override
    public int depth() {
        return depth;
    }

    @Override
    public boolean good() {
        return true;
    }

    @Override
    public boolean apply(Command<?> command, CommandSender sender) {
        throw new RuntimeException("ParseCheckResult cannot be applied! This is a bug.");
    }

    @Override
    public CheckResult prepend(String argumentType, Object parsedArg, boolean include) {
        throw new RuntimeException("Cannot prepend to ParseCheckResult! This is a bug.");
    }
}
