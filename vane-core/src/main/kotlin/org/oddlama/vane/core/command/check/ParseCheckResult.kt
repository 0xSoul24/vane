package org.oddlama.vane.core.command.check

import org.bukkit.command.CommandSender
import org.oddlama.vane.core.command.Command

class ParseCheckResult(
    private val depth: Int,
    val argumentType: String,
    val parsed: Any?,
    val includeParam: Boolean
) : CheckResult {
    override fun depth(): Int = depth
    override fun good(): Boolean = true

    override fun apply(command: Command<*>?, sender: CommandSender?): Boolean =
        error("ParseCheckResult cannot be applied! This is a bug.")

    override fun prepend(argumentType: String, parsedArg: Any?, include: Boolean): CheckResult =
        error("Cannot prepend to ParseCheckResult! This is a bug.")
}
