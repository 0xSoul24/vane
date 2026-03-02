package org.oddlama.vane.admin

import org.oddlama.vane.admin.commands.*
import org.oddlama.vane.annotation.VaneModule
import org.oddlama.vane.core.module.Module

@VaneModule(name = "admin", bstats = 8638, configVersion = 2, langVersion = 2, storageVersion = 1)
class Admin : Module<Admin?>() {
    init {
        // Create components
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
