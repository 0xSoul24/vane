package org.oddlama.vane.core.command.check

import org.bukkit.command.CommandSender
import org.oddlama.vane.core.command.Command

class CombinedErrorCheckResult(private val errors: List<ErrorCheckResult>) :
    ErrorCheckResult(errors.first().depth(), "§6could not match one of:§r") {

    init {
        require(errors.size >= 2) {
            "Tried to create CombinedErrorCheckResult with less than 2 sub-errors! This is a bug."
        }
    }

    override fun apply(command: Command<*>?, sender: CommandSender?, indent: String): Boolean {
        super.apply(command, sender, indent)
        errors.forEach { it.apply(command, sender, "$indent  ") }
        return false
    }
}
