package org.oddlama.vane.core.command.params

import org.bukkit.command.CommandSender
import org.oddlama.vane.core.command.Command
import org.oddlama.vane.core.command.Executor
import org.oddlama.vane.core.command.Param
import org.oddlama.vane.core.command.check.CheckResult
import org.oddlama.vane.core.command.check.ErrorCheckResult
import org.oddlama.vane.core.command.check.ExecutorCheckResult
import org.oddlama.vane.core.functional.ErasedFunctor
import org.oddlama.vane.core.functional.Function1
import org.oddlama.vane.core.functional.GenericsFinder
import java.lang.reflect.Method
import java.lang.reflect.Parameter
import java.util.*
import java.util.logging.LogRecord
import java.util.stream.Collectors

class SentinelExecutorParam<T> @JvmOverloads constructor(
    command: Command<*>?,
    private val function: T?,
    private val checkRequirements: Function1<CommandSender?, Boolean?> = Function1 { _: CommandSender? -> true },
    private val skipArgumentCheck: Function1<Int?, Boolean?> = Function1 { _: Int? -> false }
) : BaseParam(command), Executor {
    private fun checkSignature(method: Method, args: MutableList<Any?>) {
        // Assert the same number of given and expected parameters
        if (args.size != method.parameters.size) {
            throw RuntimeException(
                "Invalid command functor " +
                        method.declaringClass.getName() +
                        "::" +
                        method.name +
                        "!" +
                        "\nFunctor takes " +
                        method.parameters.size +
                        " parameters, but " +
                        args.size +
                        " were given." +
                        "\nRequired: " +
                        Arrays.stream(method.parameters)
                            .map { p: Parameter? -> p!!.type.name }.toList() +
                        "\nGiven: " +
                        args.stream().map { p: Any? -> p!!.javaClass.name }.toList()
            )
        }

        // Assert assignable types
        for (i in args.indices) {
            if (skipArgumentCheck.apply(i) == true) {
                continue
            }
            val needs = method.parameters[i].type
            val got: Class<*> = args[i]!!.javaClass
            if (!needs.isAssignableFrom(got)) {
                throw RuntimeException(
                    "Invalid command functor " +
                            method.declaringClass.getName() +
                            "::" +
                            method.name +
                            "!" +
                            "\nArgument " +
                            (i + 1) +
                            " (" +
                            needs.name +
                            ") is not assignable from " +
                            got.name
                )
            }
        }
    }

    override val isExecutor: Boolean
        get() = true

    override fun execute(command: Command<*>?, sender: CommandSender?, parsedArgs: MutableList<Any?>?): Boolean {
        val args = requireNotNull(parsedArgs) { "parsedArgs must not be null" }
        val cmd = requireNotNull(command) { "command must not be null" }

        // Replace command name argument (unused) with sender
        args[0] = sender

        // Disable logger while reflecting on the lambda.
        // FIXME? This is an ugly workaround to prevent Spigot from displaying
        // a warning, that we load a class from a plugin we do not depend on,
        // but this is absolutely intended, and erroneous behavior in any way.
        val module = requireNotNull(cmd.module) { "command.module must not be null" }
        val log = requireNotNull(module.core) { "module.core must not be null" }.log
        val savedFilter = log.filter
        log.setFilter { _: LogRecord? -> false }
        // Get method reflection
        val gf = function as GenericsFinder
        val method = gf.method()
        log.setFilter(savedFilter)

        // Check method signature against given argument types
        checkSignature(method, args)

        // Check external requirements on the sender
        if (checkRequirements.apply(sender) != true) {
            return false
        }

        // Execute functor
        try {
            val result = (function as ErasedFunctor).invoke(args)
            // Map null to "true" for consuming functions
            return result == null || (result as? Boolean == true)
        } catch (e: Exception) {
            throw RuntimeException(
                "Error while invoking functor " + method.declaringClass.getName() + "::" + method.name + "!",
                e
            )
        }
    }

    override fun addParam(param: Param) {
        throw RuntimeException("Cannot add element to sentinel executor! This is a bug.")
    }

    override fun checkParse(sender: CommandSender?, args: Array<String?>?, offset: Int): CheckResult? {
        return null
    }

    override fun completionsFor(sender: CommandSender?, args: Array<String?>?, offset: Int): MutableList<String?> {
        return mutableListOf()
    }

    override fun checkAccept(sender: CommandSender?, args: Array<String?>?, offset: Int): CheckResult {
        val safeArgs = requireNotNull(args) { "args must not be null" }

        if (safeArgs.size > offset) {
            // Excess arguments are an error of the previous level, so we subtract one from the
            // offset (depth)
            // This will cause invalid arguments to be prioritized on optional arguments.
            // For example /vane reload [module], with an invalid module name should show "invalid
            // module" over
            // excess arguments.
            return ErrorCheckResult(
                offset - 1,
                "§6excess arguments: {" +
                        Arrays.stream<String?>(safeArgs, offset, safeArgs.size).map { s: String? -> "§4$s§6" }
                            .collect(
                                Collectors.joining(", ")
                            ) +
                        "}§r"
            )
        } else if (safeArgs.size < offset) {
            throw RuntimeException("Sentinel executor received missing arguments! This is a bug.")
        }
        return ExecutorCheckResult(offset, this)
    }
}
