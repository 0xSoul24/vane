package org.oddlama.vane.permissions.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.oddlama.vane.annotation.command.Name
import org.oddlama.vane.annotation.config.ConfigString
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.annotation.persistent.Persistent
import org.oddlama.vane.core.command.argumentType.OfflinePlayerArgumentType
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.permissions.Permissions
import java.util.*

@Name("vouch")
class Vouch(context: Context<Permissions?>) : org.oddlama.vane.core.command.Command<Permissions?>(context) {
    @LangMessage
    private val langVouched: TranslatedMessage? = null

    @LangMessage
    private val langAlreadyVouched: TranslatedMessage? = null

    @ConfigString(def = "user", desc = "The group to assign to players when someone vouches for them.", metrics = true)
    private val configVouchGroup: String? = null

    // Persistent storage
    @Persistent
    var storageVouchedBy: MutableMap<UUID, MutableSet<UUID>> = HashMap<UUID, MutableSet<UUID>>()

    override fun getCommandBase(): LiteralArgumentBuilder<CommandSourceStack> {
        return super.getCommandBase()
            .then(help())
            .then(
                Commands.argument<OfflinePlayer>("offline_player", OfflinePlayerArgumentType.offlinePlayer())
                    .executes { ctx: CommandContext<CommandSourceStack> ->
                        vouchForPlayer(
                            ctx.source.sender as Player,
                            ctx.getArgument("offline_player", OfflinePlayer::class.java)!!
                        )
                        Command.SINGLE_SUCCESS
                    }
            )
    }

    private fun vouchForPlayer(sender: Player, vouchedPlayer: OfflinePlayer) {
        val vouchedBySet = storageVouchedBy.computeIfAbsent(vouchedPlayer.uniqueId) { _: UUID -> HashSet<UUID>() }

        if (vouchedBySet.add(sender.uniqueId)) {
            // If it was the first one, we assign the group,
            // otherwise we just record that the player also vouched.
            if (vouchedBySet.size == 1) {
                module!!.addPlayerToGroup(vouchedPlayer, configVouchGroup)
            }

            langVouched!!.send(sender, "§b" + vouchedPlayer.name)
        } else {
            langAlreadyVouched!!.send(sender, "§b" + vouchedPlayer.name)
        }

        markPersistentStorageDirty()
    }
}
