package org.oddlama.vane.core.command.check;

import org.bukkit.command.CommandSender;
import org.oddlama.vane.core.command.Command;

public interface CheckResult {
    public int depth();

    public boolean apply(Command<?> command, CommandSender sender);

    public CheckResult prepend(String argumentType, Object parsedArg, boolean include);

    public boolean good();
}
