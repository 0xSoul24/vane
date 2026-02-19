package org.oddlama.vane.proxycore.config;

import java.util.*;
import java.util.stream.Collectors;

public class AuthMultiplex {

    public Integer port;
    private final List<UUID> allowedUuids;

    public AuthMultiplex(Integer port, List<String> allowedUuids) {
        this.port = port;

        if (allowedUuids == null || allowedUuids.isEmpty()) {
            this.allowedUuids = List.of();
        } else {
            this.allowedUuids = allowedUuids
                .stream()
                .filter(s -> !s.isEmpty())
                .map(UUID::fromString)
                .collect(Collectors.toList());
        }
    }

    public boolean uuidIsAllowed(UUID uuid) {
        return allowedUuids.isEmpty() || allowedUuids.contains(uuid);
    }
}
