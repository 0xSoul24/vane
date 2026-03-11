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
    fun ignoreCase(): FixedParam<T> = apply { ignoreCase = true }

    /** Will pass this fixed parameter as an argument to the executed function  */
    fun includeParam(): FixedParam<T> = apply { includeParam = true }

    override fun checkParse(sender: CommandSender?, args: Array<String?>?, offset: Int): CheckResult {
        if (args == null || args.size <= offset)
            return ErrorCheckResult(offset, "§6missing argument: §3$fixedArgStr§r")
        val argVal = args[offset]
            ?: return ErrorCheckResult(offset, "§6invalid argument: expected §3$fixedArgStr§6 got §bnull§r")
        return parse(argVal)
            ?.let { ParseCheckResult(offset, fixedArgStr ?: "", it, includeParam) }
            ?: ErrorCheckResult(offset, "§6invalid argument: expected §3$fixedArgStr§6 got §b$argVal§r")
    }

    override fun completionsFor(sender: CommandSender?, args: Array<String?>?, offset: Int): MutableList<String?> =
        mutableListOf(fixedArgStr)

    private fun parse(arg: String): T? =
        fixedArg.takeIf { arg.equals(fixedArgStr, ignoreCase = ignoreCase) }
}
