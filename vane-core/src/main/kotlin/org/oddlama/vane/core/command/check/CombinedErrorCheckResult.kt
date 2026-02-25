package org.oddlama.vane.core.command.check

import org.bukkit.command.CommandSender
import org.oddlama.vane.core.command.Command

class CombinedErrorCheckResult(errors: MutableList<ErrorCheckResult>) :
    ErrorCheckResult(errors[0].depth(), "§6could not match one of:§r") {
    private val errors: MutableList<ErrorCheckResult>

    init {
        if (errors.size < 2) {
            throw RuntimeException(
                "Tried to create CombinedErrorCheckResult with less than 2 sub-errors! This is a bug."
            )
        }
        this.errors = errors
    }

    override fun apply(command: Command<*>?, sender: CommandSender?, indent: String?): Boolean {
        super.apply(command, sender, indent)
        for (err in errors) {
            err.apply(command, sender, "$indent  ")
        }
        return false
    }
}
