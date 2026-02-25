package org.oddlama.vane.core.command

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.GameMode
import org.bukkit.OfflinePlayer
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.permissions.Permission
import org.oddlama.vane.core.command.check.CheckResult
import org.oddlama.vane.core.command.check.CombinedErrorCheckResult
import org.oddlama.vane.core.command.check.ErrorCheckResult
import org.oddlama.vane.core.command.check.ParseCheckResult
import org.oddlama.vane.core.command.params.*
import org.oddlama.vane.core.functional.*
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.util.StorageUtil.namespacedKey
import java.util.*
import java.util.stream.Collectors

interface Param {
    val params: MutableList<Param?>?

    fun requirePlayer(sender: CommandSender?): Boolean {
        if (sender !is Player) {
            this.command!!.module!!.core?.langCommandNotAPlayer?.send(sender)
            return false
        }

        return true
    }

    val isExecutor: Boolean
        get() = false

    fun addParam(param: Param) {
        if (param.isExecutor && this.params!!.stream().anyMatch { p: Param? -> p!!.isExecutor }) {
            throw RuntimeException("Cannot define multiple executors for the same parameter! This is a bug.")
        }
        this.params!!.add(param)
    }

    fun <T1> execPlayer(f: Consumer1<T1?>?) {
        addParam(
            SentinelExecutorParam<Consumer1<T1?>?>(
                this.command,
                f,
                { sender: CommandSender? -> this.requirePlayer(sender) },
                { i: Int? -> i == 0 })
        )
    }

    fun <T1, T2> execPlayer(f: Consumer2<T1?, T2?>?) {
        addParam(
            SentinelExecutorParam<Consumer2<T1?, T2?>?>(
                this.command,
                f,
                { sender: CommandSender? -> this.requirePlayer(sender) },
                { i: Int? -> i == 0 })
        )
    }

    fun <T1, T2, T3> execPlayer(f: Consumer3<T1?, T2?, T3?>?) {
        addParam(
            SentinelExecutorParam<Consumer3<T1?, T2?, T3?>?>(
                this.command,
                f,
                { sender: CommandSender? -> this.requirePlayer(sender) },
                { i: Int? -> i == 0 })
        )
    }

    fun <T1, T2, T3, T4> execPlayer(f: Consumer4<T1?, T2?, T3?, T4?>?) {
        addParam(
            SentinelExecutorParam<Consumer4<T1?, T2?, T3?, T4?>?>(
                this.command,
                f,
                { sender: CommandSender? -> this.requirePlayer(sender) },
                { i: Int? -> i == 0 })
        )
    }

    fun <T1, T2, T3, T4, T5> execPlayer(f: Consumer5<T1?, T2?, T3?, T4?, T5?>?) {
        addParam(
            SentinelExecutorParam<Consumer5<T1?, T2?, T3?, T4?, T5?>?>(
                this.command,
                f,
                { sender: CommandSender? -> this.requirePlayer(sender) },
                { i: Int? -> i == 0 })
        )
    }

    fun <T1, T2, T3, T4, T5, T6> execPlayer(f: Consumer6<T1?, T2?, T3?, T4?, T5?, T6?>?) {
        addParam(
            SentinelExecutorParam<Consumer6<T1?, T2?, T3?, T4?, T5?, T6?>?>(
                this.command,
                f,
                { sender: CommandSender? -> this.requirePlayer(sender) },
                { i: Int? -> i == 0 })
        )
    }

    fun <T1> execPlayer(f: Function1<T1?, Boolean?>?) {
        addParam(
            SentinelExecutorParam<Function1<T1?, Boolean?>?>(
                this.command,
                f,
                Function1 { sender: CommandSender? -> this.requirePlayer(sender) },
                Function1 { i: Int? -> i == 0 })
        )
    }

    fun <T1, T2> execPlayer(f: Function2<T1?, T2?, Boolean?>?) {
        addParam(
            SentinelExecutorParam<Function2<T1?, T2?, Boolean?>?>(
                this.command,
                f,
                Function1 { sender: CommandSender? -> this.requirePlayer(sender) },
                Function1 { i: Int? -> i == 0 })
        )
    }

    fun <T1, T2, T3> execPlayer(f: Function3<T1?, T2?, T3?, Boolean?>?) {
        addParam(
            SentinelExecutorParam<Function3<T1?, T2?, T3?, Boolean?>?>(
                this.command,
                f,
                Function1 { sender: CommandSender? -> this.requirePlayer(sender) },
                Function1 { i: Int? -> i == 0 })
        )
    }

    fun <T1, T2, T3, T4> execPlayer(f: Function4<T1?, T2?, T3?, T4?, Boolean?>?) {
        addParam(
            SentinelExecutorParam<Function4<T1?, T2?, T3?, T4?, Boolean?>?>(
                this.command,
                f,
                Function1 { sender: CommandSender? -> this.requirePlayer(sender) },
                Function1 { i: Int? -> i == 0 })
        )
    }

    fun <T1, T2, T3, T4, T5> execPlayer(f: Function5<T1?, T2?, T3?, T4?, T5?, Boolean?>?) {
        addParam(
            SentinelExecutorParam<Function5<T1?, T2?, T3?, T4?, T5?, Boolean?>?>(
                this.command,
                f,
                Function1 { sender: CommandSender? -> this.requirePlayer(sender) },
                Function1 { i: Int? -> i == 0 })
        )
    }

    fun <T1, T2, T3, T4, T5, T6> execPlayer(f: Function6<T1?, T2?, T3?, T4?, T5?, T6?, Boolean?>?) {
        addParam(
            SentinelExecutorParam<Function6<T1?, T2?, T3?, T4?, T5?, T6?, Boolean?>?>(
                this.command,
                f,
                Function1 { sender: CommandSender? -> this.requirePlayer(sender) },
                Function1 { i: Int? -> i == 0 })
        )
    }

    fun <T1> exec(f: Consumer1<T1?>?) {
        addParam(
            SentinelExecutorParam<Consumer1<T1?>?>(
                this.command, f
            )
        )
    }

    fun <T1, T2> exec(f: Consumer2<T1?, T2?>?) {
        addParam(
            SentinelExecutorParam<Consumer2<T1?, T2?>?>(
                this.command, f
            )
        )
    }

    fun <T1, T2, T3> exec(f: Consumer3<T1?, T2?, T3?>?) {
        addParam(
            SentinelExecutorParam<Consumer3<T1?, T2?, T3?>?>(
                this.command, f
            )
        )
    }

    fun <T1, T2, T3, T4> exec(f: Consumer4<T1?, T2?, T3?, T4?>?) {
        addParam(
            SentinelExecutorParam<Consumer4<T1?, T2?, T3?, T4?>?>(
                this.command, f
            )
        )
    }

    fun <T1, T2, T3, T4, T5> exec(f: Consumer5<T1?, T2?, T3?, T4?, T5?>?) {
        addParam(
            SentinelExecutorParam<Consumer5<T1?, T2?, T3?, T4?, T5?>?>(
                this.command, f
            )
        )
    }

    fun <T1, T2, T3, T4, T5, T6> exec(f: Consumer6<T1?, T2?, T3?, T4?, T5?, T6?>?) {
        addParam(
            SentinelExecutorParam<Consumer6<T1?, T2?, T3?, T4?, T5?, T6?>?>(
                this.command, f
            )
        )
    }

    fun <T1> exec(f: Function1<T1?, Boolean?>?) {
        addParam(
            SentinelExecutorParam<Function1<T1?, Boolean?>?>(
                this.command, f
            )
        )
    }

    fun <T1, T2> exec(f: Function2<T1?, T2?, Boolean?>?) {
        addParam(
            SentinelExecutorParam<Function2<T1?, T2?, Boolean?>?>(
                this.command, f
            )
        )
    }

    fun <T1, T2, T3> exec(f: Function3<T1?, T2?, T3?, Boolean?>?) {
        addParam(
            SentinelExecutorParam<Function3<T1?, T2?, T3?, Boolean?>?>(
                this.command, f
            )
        )
    }

    fun <T1, T2, T3, T4> exec(f: Function4<T1?, T2?, T3?, T4?, Boolean?>?) {
        addParam(
            SentinelExecutorParam<Function4<T1?, T2?, T3?, T4?, Boolean?>?>(
                this.command, f
            )
        )
    }

    fun <T1, T2, T3, T4, T5> exec(f: Function5<T1?, T2?, T3?, T4?, T5?, Boolean?>?) {
        addParam(
            SentinelExecutorParam<Function5<T1?, T2?, T3?, T4?, T5?, Boolean?>?>(
                this.command, f
            )
        )
    }

    fun <T1, T2, T3, T4, T5, T6> exec(f: Function6<T1?, T2?, T3?, T4?, T5?, T6?, Boolean?>?) {
        addParam(
            SentinelExecutorParam<Function6<T1?, T2?, T3?, T4?, T5?, T6?, Boolean?>?>(
                this.command, f
            )
        )
    }

    fun anyString(): Param {
        return any<String?>("string", Function1 { str: String? -> str })
    }

    fun <T> any(argumentType: String?, fromString: Function1<String?, out T?>): AnyParam<out T?> {
        val p: AnyParam<out T?> = AnyParam<T?>(
            this.command, argumentType, fromString
        )
        addParam(p)
        return p
    }

    fun fixed(fixed: String?): FixedParam<String?> {
        return fixed<String?>(fixed) { str: String? -> str }
    }

    fun <T> fixed(fixed: T?, toString: Function1<T?, String?>): FixedParam<T?> {
        val p = FixedParam<T?>(
            this.command, fixed, toString
        )
        addParam(p)
        return p
    }

    fun choice(choices: MutableCollection<String?>): Param {
        return choice<String?>("choice", choices) { str: String? -> str }
    }

    fun <T> choice(
        argumentType: String?,
        choices: MutableCollection<out T?>,
        toString: Function1<T?, String?>
    ): ChoiceParam<T?> {
        val p = ChoiceParam<T?>(
            this.command, argumentType, choices, toString
        )
        addParam(p)
        return p
    }

    fun <T> choice(
        argumentType: String?,
        choices: Function1<CommandSender?, MutableCollection<out T?>?>,
        toString: Function2<CommandSender?, T?, String?>,
        fromString: Function2<CommandSender?, String?, out T?>
    ): DynamicChoiceParam<T?> {
        val p = DynamicChoiceParam<T?>(
            this.command, argumentType, choices, toString, fromString
        )
        addParam(p)
        return p
    }

    fun chooseModule(): DynamicChoiceParam<Module<*>?> {
        return choice<Module<*>?>(
            "module",
            Function1 { sender: CommandSender? -> this.command!!.module!!.core?.modules ?: mutableSetOf<Module<*>>() },
            Function2 { sender: CommandSender?, m: Module<*>? -> m!!.annotationName },
            Function2 { sender: CommandSender?, str: String? ->
                val core = this.command!!.module!!.core ?: return@Function2 null
                core.modules
                    .stream()
                    .filter { k: Module<*>? -> k!!.annotationName.equals(str, ignoreCase = true) }
                    .findFirst()
                    .orElse(null)
            }
        )
    }

    fun chooseWorld(): DynamicChoiceParam<World?> {
        return choice<World?>(
            "world",
            { sender: CommandSender? -> this.command!!.module!!.server.worlds },
            { sender: CommandSender?, w: World? -> w!!.name.lowercase(Locale.getDefault()) },
            Function2 { sender: CommandSender?, str: String? ->
                this.command!!
                    .module!!
                    .server
                    .worlds
                    .stream()
                    .filter { w: World? -> w!!.name.equals(str, ignoreCase = true) }
                    .findFirst()
                    .orElse(null)
            }
        )
    }

    fun chooseAnyPlayer(): DynamicChoiceParam<OfflinePlayer?> {
        return choice<OfflinePlayer?>(
            "any_player",
            { sender: CommandSender? -> this.command!!.module!!.offlinePlayersWithValidName },
            { sender: CommandSender?, p: OfflinePlayer? -> p!!.name },
            Function2 { sender: CommandSender?, str: String? ->
                this.command!!
                    .module!!
                    .offlinePlayersWithValidName
                    .stream()
                    .filter { k: OfflinePlayer? -> k!!.name.equals(str, ignoreCase = true) }
                    .findFirst()
                    .orElse(null)
            }
        )
    }

    fun chooseOnlinePlayer(): DynamicChoiceParam<Player?> {
        return choice<Player?>(
            "online_player",
            { sender: CommandSender? -> this.command!!.module!!.server.onlinePlayers },
            { sender: CommandSender?, p: Player? -> p!!.name },
            Function2 { sender: CommandSender?, str: String? ->
                this.command!!
                    .module!!
                    .server
                    .onlinePlayers
                    .stream()
                    .filter { k: Player? -> k!!.name.equals(str, ignoreCase = true) }
                    .findFirst()
                    .orElse(null)
            }
        )
    }

    // TODO (minor): Make choosePermission filter results based on the previously
    // specified player.
    fun choosePermission(): DynamicChoiceParam<Permission?> {
        return choice<Permission?>(
            "permission",
            { sender: CommandSender? ->
                this.command!!.module!!.server.pluginManager.permissions
            },
            { sender: CommandSender?, p: Permission? -> p!!.name },
            Function2 { sender: CommandSender?, str: String? ->
                this.command!!.module!!.server.pluginManager.getPermission(str!!)
            }
        )
    }

    fun chooseGamemode(): ChoiceParam<GameMode?> {
        return choice<GameMode?>(
            "gamemode",
            mutableListOf<GameMode?>(*GameMode.entries.toTypedArray())
        ) { m: GameMode? -> m!!.name.lowercase(Locale.getDefault()) }.ignoreCase()
    }

    fun chooseEnchantment(
        filter: Function2<CommandSender?, Enchantment?, Boolean?> = Function2 { sender: CommandSender?, e: Enchantment? -> true }
    ): DynamicChoiceParam<Enchantment?> {
        return choice<Enchantment?>(
            "enchantment",
            { sender: CommandSender? ->
                RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.ENCHANTMENT)
                    .stream()
                    .filter { e: Enchantment? -> filter.apply(sender, e) == true }
                    .collect(Collectors.toList())
            },
            { sender: CommandSender?, e: Enchantment? -> e!!.key.toString() },
            Function2 { sender: CommandSender?, str: String? ->
                val parts: Array<String?> = str?.split(":".toRegex())?.dropLastWhile { it.isEmpty() }?.toTypedArray() ?: return@Function2 null
                if (parts.size != 2) {
                    return@Function2 null
                }
                val e: Enchantment? = RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.ENCHANTMENT)
                    .get(namespacedKey(parts[0]!!, parts[1]!!))
                if (filter.apply(sender, e) != true) {
                    return@Function2 null
                }
                e
            }
        )
    }

    fun checkAcceptDelegate(sender: CommandSender?, args: Array<String?>?, offset: Int): CheckResult? {
        if (this.params!!.isEmpty()) {
            throw RuntimeException("Encountered parameter without sentinel! This is a bug.")
        }

        // Delegate to children
        val results =
            this.params!!.stream().map<CheckResult> { p: Param? -> p!!.checkAccept(sender, args, offset + 1) }.toList()

        // Return the first executor result, if any
        for (r in results) {
            if (r.good()) {
                return r
            }
        }

        // Only retain errors from maximum depth
        val maxDepth =
            results.stream().map { r: CheckResult -> r.depth() }.reduce(0) { a: Int, b: Int -> Integer.max(a, b) }

        val errors = results
            .stream()
            .filter { r: CheckResult -> r.depth() == maxDepth }
            .map { obj: CheckResult -> ErrorCheckResult::class.java.cast(obj) }
            .collect(Collectors.toList())

        // If there is only a single max-depth sub-error, propagate it.
        // Otherwise, combine multiple errors into new error.
        return if (errors.size == 1) {
            errors[0]
        } else {
            CombinedErrorCheckResult(errors)
        }
    }

    fun checkAccept(sender: CommandSender?, args: Array<String?>?, offset: Int): CheckResult? {
        val result = checkParse(sender, args, offset)
        if (result !is ParseCheckResult) {
            return result
        }

        return checkAcceptDelegate(sender, args, offset)!!.prepend(
            result.argumentType(),
            result.parsed(), result.includeParam())
    }

    fun buildCompletionsDelegate(sender: CommandSender?, args: Array<String?>, offset: Int): MutableList<String?> {
        if (this.params!!.isEmpty()) {
            throw RuntimeException("Encountered parameter without sentinel! This is a bug.")
        }

        // Delegate to children
        return this.params!!
            .stream()
            .map<MutableList<String?>?> { p: Param? -> p!!.buildCompletions(sender, args, offset + 1) }
            .flatMap<String?> { obj: MutableList<String?>? -> obj!!.stream() }
            .collect(Collectors.toList())
    }

    fun buildCompletions(sender: CommandSender?, args: Array<String?>, offset: Int): MutableList<String?>? {
        return if (offset < args.size - 1) {
            // We are not the last argument.
            // Delegate completion to children if the param accepts the given arguments,
            // return no completions if it doesn't
            if (checkParse(sender, args, offset) is ParseCheckResult) {
                buildCompletionsDelegate(sender, args, offset)
            } else {
                mutableListOf()
            }
        } else {
            // We are the parameter that needs to be completed.
            // Offer (partial) completions if our depth is the completion depth
            completionsFor(sender, args, offset)
        }
    }

    fun completionsFor(sender: CommandSender?, args: Array<String?>?, offset: Int): MutableList<String?>?

    fun checkParse(sender: CommandSender?, args: Array<String?>?, offset: Int): CheckResult?

    val command: Command<*>?
}
