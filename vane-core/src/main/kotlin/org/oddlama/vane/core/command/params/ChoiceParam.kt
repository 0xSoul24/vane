package org.oddlama.vane.core.command.params

import org.bukkit.command.CommandSender
import org.oddlama.vane.core.command.Command
import org.oddlama.vane.core.command.check.CheckResult
import org.oddlama.vane.core.command.check.ErrorCheckResult
import org.oddlama.vane.core.command.check.ParseCheckResult
import org.oddlama.vane.core.functional.Function1

class ChoiceParam<T>(
    command: Command<*>?,
    private val argumentType: String,
    private val choices: Collection<T?>,
    private val toString: Function1<T?, String?>
) : BaseParam(command) {
    private var ignoreCase = false
    private val fromString: MutableMap<String, T?> = choices.associateByTo(mutableMapOf()) { (toString.apply(it) ?: "") }

    /** Will ignore the case of the given argument when matching  */
    fun ignoreCase(): ChoiceParam<T> {
        ignoreCase = true
        fromString.clear()
        choices.forEach { fromString[toString.apply(it)?.lowercase() ?: ""] = it }
        return this
    }

    override fun checkParse(sender: CommandSender?, args: Array<String?>?, offset: Int): CheckResult {
        if (args == null || args.size <= offset)
            return ErrorCheckResult(offset, "§6missing argument: §3$argumentType§r")
        val argVal = args[offset]
            ?: return ErrorCheckResult(offset, "§6invalid §3$argumentType§6: §bnull§r")
        return parse(argVal)
            ?.let { ParseCheckResult(offset, argumentType, it, true) }
            ?: ErrorCheckResult(offset, "§6invalid §3$argumentType§6: §b$argVal§r")
    }

    override fun completionsFor(sender: CommandSender?, args: Array<String?>?, offset: Int): MutableList<String?> {
        val query = args?.getOrNull(offset) ?: return mutableListOf()
        val search = if (ignoreCase) query.lowercase() else query
        return choices
            .asSequence()
            .mapNotNull { toString.apply(it) }
            .filter { str -> if (ignoreCase) str.lowercase().contains(search) else str.contains(search) }
            .toMutableList()
    }

    private fun parse(arg: String): T? =
        fromString[if (ignoreCase) arg.lowercase() else arg]
}
