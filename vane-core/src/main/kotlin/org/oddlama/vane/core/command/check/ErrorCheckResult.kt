package org.oddlama.vane.core.command.check

import org.bukkit.command.CommandSender
import org.oddlama.vane.core.command.Command

open class ErrorCheckResult(private val depth: Int, private val message: String) : CheckResult {
    private var argChain = ""

    override fun depth(): Int = depth
    override fun good(): Boolean = false

    override fun apply(command: Command<*>?, sender: CommandSender?): Boolean =
        apply(command, sender, "")

    open fun apply(command: Command<*>?, sender: CommandSender?, indent: String): Boolean {
        val prefix = indent.ifEmpty { "${indent}§cerror: " }
        sender?.sendMessage("${prefix}§6${argChain}${message}")
        return false
    }

    override fun prepend(argumentType: String, parsedArg: Any?, include: Boolean): CheckResult {
        argChain = "§3${argumentType}§6 → $argChain"
        return this
    }
}
