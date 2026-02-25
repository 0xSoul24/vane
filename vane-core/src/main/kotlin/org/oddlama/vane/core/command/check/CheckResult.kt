package org.oddlama.vane.core.command.check

import org.bukkit.command.CommandSender
import org.oddlama.vane.core.command.Command

interface CheckResult {
    fun depth(): Int

    fun apply(command: Command<*>?, sender: CommandSender?): Boolean

    fun prepend(argumentType: String?, parsedArg: Any?, include: Boolean): CheckResult?

    fun good(): Boolean
}
