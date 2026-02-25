package org.oddlama.vane.core.command

import org.bukkit.command.CommandSender

interface Executor {
    fun execute(command: Command<*>?, sender: CommandSender?, parsedArgs: MutableList<Any?>?): Boolean
}
