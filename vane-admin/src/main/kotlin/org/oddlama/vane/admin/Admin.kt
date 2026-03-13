package org.oddlama.vane.admin

import org.oddlama.vane.admin.commands.Autostop
import org.oddlama.vane.admin.commands.Gamemode
import org.oddlama.vane.admin.commands.SlimeChunk
import org.oddlama.vane.admin.commands.Time
import org.oddlama.vane.admin.commands.Weather
import org.oddlama.vane.annotation.VaneModule
import org.oddlama.vane.core.module.Module

/**
 * Root module for administrative gameplay utilities and commands.
 */
@VaneModule(name = "admin", bstats = 8638, configVersion = 2, langVersion = 2, storageVersion = 1)
class Admin : Module<Admin?>() {
    init {
        Gamemode(this)
        SlimeChunk(this)
        Time(this)
        Weather(this)

        val autostopGroup = AutostopGroup(this)
        AutostopListener(autostopGroup)
        Autostop(autostopGroup)

        SpawnProtection(this)
        WorldProtection(this)
        HazardProtection(this)
        ChatMessageFormatter(this)
    }
}
