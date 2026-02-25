package org.oddlama.vane.core.command.params

import org.bukkit.command.CommandSender
import org.oddlama.vane.core.command.Command
import org.oddlama.vane.core.command.check.CheckResult
import org.oddlama.vane.core.command.check.ErrorCheckResult
import org.oddlama.vane.core.command.check.ParseCheckResult
import org.oddlama.vane.core.functional.Function1
import java.util.*

class ChoiceParam<T>(
    command: Command<*>?,
    private val argumentType: String?,
    private val choices: MutableCollection<out T?>,
    private val toString: Function1<T?, String?>
) : BaseParam(command) {
    private val fromString = HashMap<String?, T?>()
    private var ignoreCase = false

    init {
        for (c in choices) {
            fromString[toString.apply(c)] = c
        }
    }

    /** Will ignore the case of the given argument when matching  */
    fun ignoreCase(): ChoiceParam<T> {
        this.ignoreCase = true
        fromString.clear()
        for (c in choices) {
            val key = toString.apply(c)?.lowercase(Locale.getDefault())
            fromString[key] = c
        }
        return this
    }

    override fun checkParse(sender: CommandSender?, args: Array<String?>?, offset: Int): CheckResult {
        if (args == null || args.size <= offset) {
            return ErrorCheckResult(offset, "§6missing argument: §3$argumentType§r")
        }
        val argVal = args[offset] ?: return ErrorCheckResult(
            offset,
            "§6invalid §3$argumentType§6: §bnull§r"
        )
        val parsed = parse(argVal) ?: return ErrorCheckResult(
            offset,
            "§6invalid §3$argumentType§6: §b$argVal§r"
        )
        return ParseCheckResult(offset, argumentType, parsed, true)
    }

    override fun completionsFor(sender: CommandSender?, args: Array<String?>?, offset: Int): MutableList<String?> {
        val query = args?.getOrNull(offset) ?: return mutableListOf()
        val search = if (ignoreCase) query.lowercase(Locale.getDefault()) else query
        val results = ArrayList<String?>()
        for (c in choices) {
            val str = toString.apply(c) ?: continue
            val matches = if (ignoreCase) str.lowercase(Locale.getDefault()).contains(search) else str.contains(search)
            if (matches) results.add(str)
        }
        return results
    }

    private fun parse(arg: String): T? {
        return if (ignoreCase) {
            fromString[arg.lowercase(Locale.getDefault())]
        } else {
            fromString[arg]
        }
    }
}
