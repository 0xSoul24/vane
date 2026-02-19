package org.oddlama.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

public class Util {

    public static RegisteredServer getServerForHost(ProxyServer proxy, InetSocketAddress host) {
        Map<String, List<String>> forcedHosts = proxy.getConfiguration().getForcedHosts();

        String forced;
        RegisteredServer server;
        try {
            forced = forcedHosts.get(host.getHostString()).get(0);
            if (forced == null || forced.isEmpty()) throw new Exception();
            server = proxy.getServer(forced).get();
        } catch (Exception ignored) {
            server = proxy.getServer(proxy.getConfiguration().getAttemptConnectionOrder().get(0)).get();
        }

        return server;
    }
}
