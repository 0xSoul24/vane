package org.oddlama.vane.core.command.params

import org.bukkit.command.CommandSender
import org.oddlama.vane.core.command.Command
import org.oddlama.vane.core.command.check.CheckResult
import org.oddlama.vane.core.command.check.ErrorCheckResult
import org.oddlama.vane.core.command.check.ParseCheckResult
import org.oddlama.vane.core.functional.Function1
import org.oddlama.vane.core.functional.Function2

class DynamicChoiceParam<T>(
    command: Command<*>?,
    private val argumentType: String,
    private val choices: Function1<CommandSender?, Collection<T?>?>,
    private val toString: Function2<CommandSender?, T?, String?>,
    private val fromString: Function2<CommandSender?, String?, out T?>
) : BaseParam(command) {

    override fun checkParse(sender: CommandSender?, args: Array<String?>?, offset: Int): CheckResult {
        if (args == null || args.size <= offset)
            return ErrorCheckResult(offset, "§6missing argument: §3$argumentType§r")
        return fromString.apply(sender, args[offset])
            ?.let { ParseCheckResult(offset, argumentType, it, true) }
            ?: ErrorCheckResult(offset, "§6invalid §3$argumentType§6: §b${args[offset]}§r")
    }

    override fun completionsFor(sender: CommandSender?, args: Array<String?>?, offset: Int): MutableList<String?> {
        val current = args?.getOrNull(offset)?.lowercase() ?: return mutableListOf()
        return choices.apply(sender)
            .orEmpty()
            .asSequence()
            .mapNotNull { toString.apply(sender, it) }
            .filter { it.lowercase().contains(current) }
            .toMutableList()
    }
}
