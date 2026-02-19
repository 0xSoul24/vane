package org.oddlama.vane.core.command.check;

import java.util.Objects;
import org.bukkit.command.CommandSender;
import org.oddlama.vane.core.command.Command;

public class ErrorCheckResult implements CheckResult {

    private int depth;
    private String message;
    private String argChain = "";

    public ErrorCheckResult(int depth, String message) {
        this.depth = depth;
        this.message = message;
    }

    @Override
    public int depth() {
        return depth;
    }

    @Override
    public boolean good() {
        return false;
    }

    @Override
    public boolean apply(Command<?> command, CommandSender sender) {
        return apply(command, sender, "");
    }

    public boolean apply(Command<?> command, CommandSender sender, String indent) {
        var str = indent;
        if (Objects.equals(indent, "")) {
            str += "§cerror: ";
        }
        str += "§6";
        str += argChain;
        str += message;
        sender.sendMessage(str);
        return false;
    }

    @Override
    public CheckResult prepend(String argumentType, Object parsedArg, boolean include) {
        // Save parsed arguments in an argument chain, and propagate error
        argChain = "§3" + argumentType + "§6 → " + argChain;
        return this;
    }
}
