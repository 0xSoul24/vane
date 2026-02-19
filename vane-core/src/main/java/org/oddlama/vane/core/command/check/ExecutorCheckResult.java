package org.oddlama.vane.core.command.check;

import java.util.ArrayList;
import org.bukkit.command.CommandSender;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.command.Executor;

public class ExecutorCheckResult implements CheckResult {

    private int depth;
    private Executor executor;
    private ArrayList<Object> parsedArgs = new ArrayList<>();

    public ExecutorCheckResult(int depth, Executor executor) {
        this.depth = depth;
        this.executor = executor;
    }

    @Override
    public int depth() {
        return depth;
    }

    @Override
    public boolean good() {
        return true;
    }

    public boolean apply(Command<?> command, CommandSender sender) {
        return executor.execute(command, sender, parsedArgs);
    }

    @Override
    public CheckResult prepend(String argumentType, Object parsedArg, boolean include) {
        if (include) {
            parsedArgs.add(0, parsedArg);
        }
        return this;
    }
}
