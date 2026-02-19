package org.oddlama.vane.proxycore.commands;

import java.util.UUID;
import org.oddlama.vane.proxycore.VaneProxyPlugin;

public abstract class ProxyCommand {

    public final String permission;
    public final VaneProxyPlugin plugin;

    public ProxyCommand(String permission, VaneProxyPlugin plugin) {
        this.permission = permission;
        this.plugin = plugin;
    }

    public abstract void execute(ProxyCommandSender sender, String[] args);

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasPermission(UUID uuid) {
        return this.permission == null || plugin.getProxy().hasPermission(uuid, this.permission);
    }
}
