package org.oddlama.vane.regions.menu;

import java.util.UUID;

public class RegionGroupMenuTag {

    private final UUID regionGroupId;

    public RegionGroupMenuTag(final UUID regionGroupId) {
        this.regionGroupId = regionGroupId;
    }

    public UUID regionGroupId() {
        return regionGroupId;
    }
}
