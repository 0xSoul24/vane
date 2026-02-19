package org.oddlama.vane.admin;

import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.core.module.Module;

@VaneModule(name = "admin", bstats = 8638, configVersion = 2, langVersion = 2, storageVersion = 1)
public class Admin extends Module<Admin> {

    public Admin() {
        // Create components
        new org.oddlama.vane.admin.commands.Gamemode(this);
        new org.oddlama.vane.admin.commands.SlimeChunk(this);
        new org.oddlama.vane.admin.commands.Time(this);
        new org.oddlama.vane.admin.commands.Weather(this);

        var autostopGroup = new AutostopGroup(this);
        new AutostopListener(autostopGroup);
        new org.oddlama.vane.admin.commands.Autostop(autostopGroup);

        new SpawnProtection(this);
        new WorldProtection(this);
        new HazardProtection(this);
        new ChatMessageFormatter(this);
    }
}
