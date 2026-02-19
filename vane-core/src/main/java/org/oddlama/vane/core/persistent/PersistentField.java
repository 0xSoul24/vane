package org.oddlama.vane.core.persistent;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.function.Function;
import org.json.JSONObject;

public class PersistentField {

    private Object owner;
    private Field field;
    private String path;

    public PersistentField(Object owner, Field field, Function<String, String> mapName) {
        this.owner = owner;
        this.field = field;
        this.path = mapName.apply(field.getName().substring("storage".length()));

        field.setAccessible(true);
    }

    public String path() {
        return path;
    }

    public Object get() {
        try {
            return field.get(owner);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
        }
    }

    public void save(JSONObject json) throws IOException {
        json.put(path, PersistentSerializer.toJson(field, get()));
    }

    public void load(JSONObject json) throws IOException {
        if (!json.has(path)) {
            throw new IOException("Missing key in persistent storage: '" + path + "'");
        }

        try {
            field.set(owner, PersistentSerializer.fromJson(field, json.get(path)));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
        }
    }
}
