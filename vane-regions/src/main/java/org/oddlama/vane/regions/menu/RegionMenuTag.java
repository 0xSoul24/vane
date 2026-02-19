package org.oddlama.vane.regions.menu;

import java.util.UUID;

public class RegionMenuTag {

    private final UUID regionId;

    public RegionMenuTag(final UUID regionId) {
        this.regionId = regionId;
    }

    public UUID regionId() {
        return regionId;
    }
}
