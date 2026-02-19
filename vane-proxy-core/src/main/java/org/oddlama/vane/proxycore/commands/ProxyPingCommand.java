package org.oddlama.vane.proxycore.commands;

import org.oddlama.vane.proxycore.ProxyPlayer;
import org.oddlama.vane.proxycore.VaneProxyPlugin;

public class ProxyPingCommand extends ProxyCommand {

    public ProxyPingCommand(String permission, VaneProxyPlugin plugin) {
        super(permission, plugin);
    }

    @Override
    public void execute(ProxyCommandSender sender, String[] args) {
        if (!(sender instanceof final ProxyPlayer player)) {
            sender.sendMessage("Not a player!");
            return;
        }

        if (!hasPermission(player.getUniqueId())) {
            sender.sendMessage("No permission!");
            return;
        }

        player.sendMessage("§7ping: §3" + player.getPing() + "ms");
    }
}
