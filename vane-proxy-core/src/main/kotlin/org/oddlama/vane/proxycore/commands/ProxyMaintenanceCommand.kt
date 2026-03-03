package org.oddlama.vane.proxycore.commands

import org.oddlama.vane.proxycore.Maintenance
import org.oddlama.vane.proxycore.ProxyPlayer
import org.oddlama.vane.proxycore.VaneProxyPlugin
import org.oddlama.vane.util.TimeUtil.parseTime
import java.util.*

class ProxyMaintenanceCommand(permission: String?, plugin: VaneProxyPlugin) : ProxyCommand(permission, plugin) {
    override fun execute(sender: ProxyCommandSender?, args: Array<String?>?) {
        // Only check permission on players
        if (sender is ProxyPlayer && !hasPermission(sender.uniqueId)) {
            sender.sendMessage("No permission!")
            return
        }

        val maintenance = plugin.maintenance
        val safeArgs: Array<String?> = args ?: arrayOf()

        if (safeArgs.size == 1) {
            when (safeArgs[0]?.lowercase(Locale.getDefault())) {
                "status" -> {
                    if (maintenance.start() != 0L) {
                        sender?.sendMessage(
                            maintenance.formatMessage(Maintenance.MESSAGE_INFO)
                        )
                    }

                    return
                }

                "on" -> {
                    maintenance.schedule(System.currentTimeMillis(), null)
                    return
                }

                "cancel", "off" -> {
                    maintenance.abort()
                    return
                }
            }
        } else if (safeArgs.size == 3 && (safeArgs[0].equals("schedule", ignoreCase = true))) {
            val time: Long
            val duration: Long

            try {
                time = parseTime(safeArgs[1]!!)
            } catch (_: NumberFormatException) {
                sender?.sendMessage(MESSAGE_INVALID_TIME_FORMAT.replace("%time%", safeArgs[1]!!))
                return
            }

            try {
                duration = parseTime(safeArgs[2]!!)
            } catch (_: NumberFormatException) {
                sender?.sendMessage(MESSAGE_INVALID_TIME_FORMAT.replace("%time%", safeArgs[2]!!))
                return
            }

            maintenance.schedule(System.currentTimeMillis() + time, duration)
            return
        }

        sender?.sendMessage(
            """
            §7> §3/maintenance §3[ §7cancel§r|§7off §3] §f- Cancel any scheduled/active maintenance
            §7> §3/maintenance §3[ §7status §3] §f- Display info about scheduled/active maintenance
            §7> §3/maintenance §3[ §7on §3] §f- Enable maintenance for an indefinite amount of time
            §7> §3/maintenance §3[ §7schedule §3] §7<§bin§7> <§bduration§7> §f- Schedule maintenance in <in> for <duration>
            §7> §3|§7 time format§7 §f- Examples: §b§o3h5m§r§f or §b§o1y2w3d4h5m6s§r
            """.trimIndent()
        )
    }

    companion object {
        var MESSAGE_INVALID_TIME_FORMAT: String = "§cInvalid time format §6'%time%'§c!"
    }
}
