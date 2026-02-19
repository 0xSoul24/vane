package org.oddlama.vane.portals.menu;

import java.util.UUID;

public class PortalMenuTag {

    private final UUID portalId;

    public PortalMenuTag(final UUID portalId) {
        this.portalId = portalId;
    }

    public UUID portalId() {
        return portalId;
    }
}
