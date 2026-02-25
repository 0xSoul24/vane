package org.oddlama.vane.core.command.check

import org.bukkit.command.CommandSender
import org.oddlama.vane.core.command.Command

class ParseCheckResult(
    private val depth: Int,
    private val argumentType: String?,
    private val parsed: Any?,
    private val includeParam: Boolean
) : CheckResult {
    fun argumentType(): String? {
        return argumentType
    }

    fun parsed(): Any? {
        return parsed
    }

    fun includeParam(): Boolean {
        return includeParam
    }

    override fun depth(): Int {
        return depth
    }

    override fun good(): Boolean {
        return true
    }

    override fun apply(command: Command<*>?, sender: CommandSender?): Boolean {
        throw RuntimeException("ParseCheckResult cannot be applied! This is a bug.")
    }

    override fun prepend(argumentType: String?, parsedArg: Any?, include: Boolean): CheckResult? {
        throw RuntimeException("Cannot prepend to ParseCheckResult! This is a bug.")
    }
}
