package org.oddlama.vane.proxycore;

import java.util.Collection;
import java.util.UUID;
import org.oddlama.vane.proxycore.scheduler.ProxyTaskScheduler;

public interface ProxyServer {
    ProxyTaskScheduler getScheduler();

    void broadcast(String message);

    Collection<ProxyPlayer> getPlayers();

    boolean hasPermission(UUID uuid, String... permission);
}
