package org.oddlama.vane.core.command.check

import org.bukkit.command.CommandSender
import org.oddlama.vane.core.command.Command

open class ErrorCheckResult(private val depth: Int, private val message: String?) : CheckResult {
    private var argChain = ""

    override fun depth(): Int {
        return depth
    }

    override fun good(): Boolean {
        return false
    }

    // Match the interface: sender may be nullable
    override fun apply(command: Command<*>?, sender: CommandSender?): Boolean {
        return apply(command, sender, "")
    }

    // Also accept nullable sender here; handle null safely
    open fun apply(command: Command<*>?, sender: CommandSender?, indent: String?): Boolean {
        var str = indent ?: ""
        if (indent == "") {
            str += "§cerror: "
        }
        str += "§6"
        str += argChain
        str += message
        sender?.sendMessage(str)
        return false
    }

    // Match the interface: argumentType may be nullable
    override fun prepend(argumentType: String?, parsedArg: Any?, include: Boolean): CheckResult? {
        // Save parsed arguments in an argument chain, and propagate error
        argChain = "§3${argumentType ?: "null"}§6 → $argChain"
        return this
    }
}
