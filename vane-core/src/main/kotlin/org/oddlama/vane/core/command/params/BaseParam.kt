package org.oddlama.vane.core.command.params

import org.oddlama.vane.core.command.Command
import org.oddlama.vane.core.command.Param

abstract class BaseParam(override val command: Command<*>?) : Param {
    override val params: MutableList<Param?>? = ArrayList()
}