package org.oddlama.vane.core.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.command.CommandSender
import org.bukkit.command.PluginIdentifiableCommand
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.bukkit.plugin.Plugin
import org.oddlama.vane.annotation.command.Aliases
import org.oddlama.vane.annotation.command.Name
import org.oddlama.vane.annotation.command.VaneCommand
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.command.params.AnyParam
import org.oddlama.vane.core.functional.Function1
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.core.module.ModuleComponent
import java.util.*

@VaneCommand
abstract class Command<T : Module<T?>?> @JvmOverloads constructor(
    context: Context<T?>,
    permissionDefault: PermissionDefault? = PermissionDefault.OP
) : ModuleComponent<T?>(null) {
    inner class BukkitCommand(name: String) : org.bukkit.command.Command(name), PluginIdentifiableCommand {
        init {
            permission = this@Command.permission.name
        }

        override fun getUsage(): String {
            return this@Command.langUsage.str("§7/§3$name")
        }

        override fun getDescription(): String {
            return this@Command.langDescription.str()
        }

        override fun getPlugin(): Plugin {
            return this@Command.module!!
        }

        // Adapted to match org.bukkit.command.Command: execute(CommandSender, String, Array<String>)
        @Throws(IllegalArgumentException::class)
        override fun execute(sender: CommandSender, commandLabel: String, args: Array<String>): Boolean {
            println("exec $commandLabel from $sender")
            // Pre-check permission
            if (!sender.hasPermission(this@Command.permission)) {
                module!!.core?.langCommandPermissionDenied?.send(sender)
                println("no perms!")
                return true
            }

            // Combine label and args into a nullable array expected by rootParam
            val combined = arrayOfNulls<String>(args.size + 1)
            combined[0] = commandLabel
            for (i in args.indices) combined[i + 1] = args[i]

            // Ambiguous matches will always execute the
            // first chain based on definition order.
            try {
                val check = rootParam.checkAccept(sender, combined, 0)
                return check?.apply(this@Command, sender) ?: false
            } catch (e: Exception) {
                sender.sendMessage(
                    "§cAn unexpected error occurred. Please examine the console log and/or notify a server administrator."
                )
                throw e
            }
        }

        @Throws(IllegalArgumentException::class)
        override fun tabComplete(sender: CommandSender, alias: String, args: Array<String>): MutableList<String> {
            // Don't allow information exfiltration!
            if (!sender.hasPermission(permission!!)) {
                return mutableListOf()
            }

            // Combine label and args into a nullable array expected by rootParam
            val combined = arrayOfNulls<String>(args.size + 1)
            combined[0] = alias
            for (i in args.indices) combined[i + 1] = args[i]

            try {
                // rootParam.buildCompletions may return MutableList<String?>; convert to non-nullable list
                val completionsNullable = rootParam.buildCompletions(sender, combined, 0) ?: mutableListOf()
                val completions: MutableList<String> = mutableListOf()
                for (c in completionsNullable) {
                    if (c != null) completions.add(c)
                }
                return completions
            } catch (e: Exception) {
                sender.sendMessage(
                    "§cAn unexpected error occurred. Please examine the console log and/or notify a server administrator."
                )
                throw e
            }
        }
    }

    // Language
    @LangMessage
    lateinit var langUsage: TranslatedMessage

    @LangMessage
    lateinit var langDescription: TranslatedMessage

    @LangMessage
    lateinit var langHelp: TranslatedMessage

    // Variables
    val name: String
    private val permission: Permission
    private var bukkitCommand: BukkitCommand

    // Root parameter
    private val rootParam: AnyParam<String?>

    // Backing field for command base to avoid generating a conflicting JVM getter
    private var commandBaseField: LiteralArgumentBuilder<CommandSourceStack>? = null
    private var aliases: Aliases?

    // Backwards-compatible getter for subclasses that override getCommandBase()
    open fun getCommandBase(): LiteralArgumentBuilder<CommandSourceStack> {
        return commandBaseField!!
    }

    init {
        var context = context
        // Make namespace
        name = javaClass.getAnnotation(Name::class.java).value
        // Convert name to PascalCase for the group name
        val groupName = "Command" + name.substring(0, 1).uppercase(Locale.getDefault()) + name.substring(1)
        context = context.group(groupName, "Enable command $name")
        setContext(context)

        // Register permission
        permission = Permission(
            "vane." + module!!.annotationName + ".commands." + name,
            "Allow access to /$name",
            permissionDefault
        )
        module!!.registerPermission(permission)
        module!!.permissionCommandCatchallModule?.let { permission.addParent(it, true) }
        module!!.core?.let { permission.addParent(it.permissionCommandCatchall, true) }

        // Always allow the console to execute commands
        module!!.addConsolePermission(permission)

        // Initialize root parameter
        rootParam = AnyParam(this, "/" + this.name, Function1 { str: String? -> str })

        // Create bukkit command
        bukkitCommand = BukkitCommand(name)
        bukkitCommand.setLabel(name)
        bukkitCommand.setName(name)

        aliases = javaClass.getAnnotation(Aliases::class.java)
        this.commandBaseField = Commands.literal(name)
        if (aliases != null) {
            bukkitCommand.setAliases(aliases!!.value.toMutableList())
        }
    }

    fun getBukkitCommand(): BukkitCommand {
        return bukkitCommand
    }

    fun getPermission(): String {
        return permission.name
    }

    val prefix: String
        get() = "vane:" + module!!.annotationName

    fun params(): Param {
        return rootParam
    }

    val command: LiteralCommandNode<CommandSourceStack>
        get() {
            val cmd =
                getCommandBase()
            val oldRequirement =
                cmd.getRequirement()
            return cmd
                .requires { stack: CommandSourceStack ->
                    stack.sender.hasPermission(permission) && oldRequirement.test(stack)
                }
                .build()
        }

    fun getAliases(): MutableList<String> {
        return if (aliases != null && aliases!!.value.isNotEmpty()) {
            aliases!!.value.toMutableList()
        } else {
            mutableListOf()
        }
    }

    override fun onEnable() {
        module!!.registerCommand(this)
    }

    override fun onDisable() {
        module!!.unregisterCommand(this)
    }

    fun printHelp(sender: CommandSender?) {
        langUsage.send(sender, "§7/§3$name")
        langHelp.send(sender)
    }

    fun printHelp2(ctx: CommandContext<CommandSourceStack>): Int {
        langUsage.send(ctx.getSource()!!.sender, "§7/§3$name")
        langHelp.send(ctx.getSource()!!.sender)
        return Command.SINGLE_SUCCESS
    }

    fun help(): LiteralArgumentBuilder<CommandSourceStack> {
        return Commands.literal("help").executes { ctx: CommandContext<CommandSourceStack> ->
            printHelp2(ctx)
            Command.SINGLE_SUCCESS
        }
    }
}
