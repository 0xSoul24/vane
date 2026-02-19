package org.oddlama.vane.portals.portal;

import static org.oddlama.vane.core.persistent.PersistentSerializer.fromJson;
import static org.oddlama.vane.core.persistent.PersistentSerializer.toJson;

import java.io.IOException;
import java.util.UUID;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.oddlama.vane.core.persistent.PersistentSerializer;
import org.oddlama.vane.util.LazyBlock;

public class PortalBlock {

    public static Object serialize(@NotNull final Object o) throws IOException {
        final var portalBlock = (PortalBlock) o;
        final var json = new JSONObject();
        json.put("block", PersistentSerializer.toJson(LazyBlock.class, portalBlock.block));
        json.put("type", PersistentSerializer.toJson(PortalBlock.Type.class, portalBlock.type));
        return json;
    }

    public static PortalBlock deserialize(@NotNull final Object o) throws IOException {
        final var json = (JSONObject) o;
        final var block = PersistentSerializer.fromJson(LazyBlock.class, json.get("block"));
        final var type = PersistentSerializer.fromJson(PortalBlock.Type.class, json.get("type"));
        return new PortalBlock(block, type);
    }

    private LazyBlock block;
    private Type type;

    public PortalBlock(final LazyBlock block, final Type type) {
        this.block = block;
        this.type = type;
    }

    public PortalBlock(final Block block, final Type type) {
        this(new LazyBlock(block), type);
    }

    public Block block() {
        return block.block();
    }

    public Type type() {
        return type;
    }

    public PortalBlockLookup lookup(final UUID portalId) {
        return new PortalBlockLookup(portalId, type);
    }

    @Override
    public int hashCode() {
        return block().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PortalBlock)) {
            return false;
        }

        final var po = (PortalBlock) other;
        // Only block is compared, as the same block can only have one functions.
        return block().equals(po.block());
    }

    public static enum Type {
        ORIGIN,
        CONSOLE,
        BOUNDARY1,
        BOUNDARY2,
        BOUNDARY3,
        BOUNDARY4,
        BOUNDARY5,
        PORTAL,
    }
}
