package org.oddlama.vane.core.command.check

import org.bukkit.command.CommandSender
import org.oddlama.vane.core.command.Command
import org.oddlama.vane.core.command.Executor

class ExecutorCheckResult(private val depth: Int, private val executor: Executor) : CheckResult {
    private val parsedArgs = mutableListOf<Any?>()

    override fun depth(): Int = depth
    override fun good(): Boolean = true

    override fun apply(command: Command<*>?, sender: CommandSender?): Boolean =
        executor.execute(command, sender, parsedArgs)

    override fun prepend(argumentType: String, parsedArg: Any?, include: Boolean): CheckResult {
        if (include) parsedArgs.add(0, parsedArg)
        return this
    }
}
