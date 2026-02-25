package org.oddlama.vane.core.command.check

import org.bukkit.command.CommandSender
import org.oddlama.vane.core.command.Command
import org.oddlama.vane.core.command.Executor

class ExecutorCheckResult(private val depth: Int, private val executor: Executor) : CheckResult {
    private val parsedArgs = ArrayList<Any?>()

    override fun depth(): Int {
        return depth
    }

    override fun good(): Boolean {
        return true
    }

    override fun apply(command: Command<*>?, sender: CommandSender?): Boolean {
        return executor.execute(command, sender, parsedArgs)
    }

    override fun prepend(argumentType: String?, parsedArg: Any?, include: Boolean): CheckResult {
        if (include) {
            parsedArgs.add(0, parsedArg)
        }
        return this
    }
}
