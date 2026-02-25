package org.oddlama.vane.core.command.params

import org.bukkit.command.CommandSender
import org.oddlama.vane.core.command.Command
import org.oddlama.vane.core.command.check.CheckResult
import org.oddlama.vane.core.command.check.ErrorCheckResult
import org.oddlama.vane.core.command.check.ParseCheckResult
import org.oddlama.vane.core.functional.Function1

class AnyParam<T>(
    command: Command<*>?,
    private val argumentType: String?,
    private val fromString: Function1<String?, out T?>
) : BaseParam(command) {
    override fun checkParse(sender: CommandSender?, args: Array<String?>?, offset: Int): CheckResult {
        if (args == null || args.size <= offset) {
            return ErrorCheckResult(offset, "§6missing argument: §3$argumentType§r")
        }
        val parsed = parse(args[offset]) ?: return ErrorCheckResult(
            offset,
            "§6invalid §3" + argumentType + "§6: §b" + args[offset] + "§r"
        )
        return ParseCheckResult(offset, argumentType, parsed, true)
    }

    override fun completionsFor(sender: CommandSender?, args: Array<String?>?, offset: Int): MutableList<String?> {
        return mutableListOf()
    }

    private fun parse(arg: String?): T? {
        return fromString.apply(arg)
    }
}
