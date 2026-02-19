package org.oddlama.vane.portals.portal;

import static org.oddlama.vane.core.persistent.PersistentSerializer.fromJson;
import static org.oddlama.vane.core.persistent.PersistentSerializer.toJson;

import java.io.IOException;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.oddlama.vane.core.persistent.PersistentSerializer;

public class PortalBlockLookup {

    public static Object serialize(@NotNull final Object o) throws IOException {
        final var lookup = (PortalBlockLookup) o;
        final var json = new JSONObject();
        json.put("portalId", PersistentSerializer.toJson(UUID.class, lookup.portalId));
        json.put("type", PersistentSerializer.toJson(PortalBlock.Type.class, lookup.type));
        return json;
    }

    public static PortalBlockLookup deserialize(@NotNull final Object o) throws IOException {
        final var json = (JSONObject) o;
        final var portalId = PersistentSerializer.fromJson(UUID.class, json.get("portalId"));
        final var type = PersistentSerializer.fromJson(PortalBlock.Type.class, json.get("type"));
        return new PortalBlockLookup(portalId, type);
    }

    private UUID portalId;
    private PortalBlock.Type type;

    public PortalBlockLookup(final UUID portalId, final PortalBlock.Type type) {
        this.portalId = portalId;
        this.type = type;
    }

    public UUID portalId() {
        return portalId;
    }

    public PortalBlock.Type type() {
        return type;
    }
}
