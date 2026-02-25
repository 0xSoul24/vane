package org.oddlama.vane.core.command.params

import org.bukkit.command.CommandSender
import org.oddlama.vane.core.command.Command
import org.oddlama.vane.core.command.check.CheckResult
import org.oddlama.vane.core.command.check.ErrorCheckResult
import org.oddlama.vane.core.command.check.ParseCheckResult
import org.oddlama.vane.core.functional.Function1

class FixedParam<T>(command: Command<*>?, private val fixedArg: T?, toString: Function1<T?, String?>) :
    BaseParam(command) {
    private val fixedArgStr: String? = toString.apply(fixedArg)
    private var includeParam = false
    private var ignoreCase = false

    /** Will ignore the case of the given argument when matching  */
    fun ignoreCase(): FixedParam<T> {
        this.ignoreCase = true
        return this
    }

    /** Will pass this fixed parameter as an argument to the executed function  */
    fun includeParam(): FixedParam<T> {
        this.includeParam = true
        return this
    }

    override fun checkParse(sender: CommandSender?, args: Array<String?>?, offset: Int): CheckResult {
        if (args == null || args.size <= offset) {
            return ErrorCheckResult(offset, "§6missing argument: §3$fixedArgStr§r")
        }
        val argVal = args[offset] ?: return ErrorCheckResult(
            offset,
            "§6invalid argument: expected §3$fixedArgStr§6 got §bnull§r"
        )
        val parsed = parse(argVal) ?: return ErrorCheckResult(
            offset,
            "§6invalid argument: expected §3$fixedArgStr§6 got §b$argVal§r"
        )
        return ParseCheckResult(offset, fixedArgStr, parsed, includeParam)
    }

    override fun completionsFor(sender: CommandSender?, args: Array<String?>?, offset: Int): MutableList<String?> {
        return mutableListOf(fixedArgStr)
    }

    private fun parse(arg: String): T? {
        if (ignoreCase) {
            if (arg.equals(fixedArgStr, ignoreCase = true)) {
                return fixedArg
            }
        } else {
            if (arg == fixedArgStr) {
                return fixedArg
            }
        }

        return null
    }
}
