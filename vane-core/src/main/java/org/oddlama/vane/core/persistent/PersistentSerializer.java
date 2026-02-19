package org.oddlama.vane.core.persistent;

import static org.oddlama.vane.util.MaterialUtil.materialFrom;
import static org.oddlama.vane.util.StorageUtil.namespacedKey;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.oddlama.vane.util.LazyBlock;
import org.oddlama.vane.util.LazyLocation;

public class PersistentSerializer {

    @FunctionalInterface
    public static interface Function<T1, R> {
        R apply(T1 t1) throws IOException;
    }

    private static Object serializeNamespacedKey(@NotNull final Object o) throws IOException {
        return ((NamespacedKey) o).toString();
    }

    private static NamespacedKey deserializeNamespacedKey(@NotNull final Object o) throws IOException {
        final var s = ((String) o).split(":");
        if (s.length != 2) {
            throw new IOException("Invalid namespaced key '" + s + "'");
        }
        return namespacedKey(s[0], s[1]);
    }

    private static Object serializeLazyLocation(@NotNull final Object o) throws IOException {
        final var lazyLocation = (LazyLocation) o;
        final var location = lazyLocation.location();
        final var json = new JSONObject();
        json.put("worldId", toJson(UUID.class, lazyLocation.worldId()));
        json.put("x", toJson(double.class, location.getX()));
        json.put("y", toJson(double.class, location.getY()));
        json.put("z", toJson(double.class, location.getZ()));
        json.put("pitch", toJson(float.class, location.getPitch()));
        json.put("yaw", toJson(float.class, location.getYaw()));
        return json;
    }

    private static LazyLocation deserializeLazyLocation(@NotNull final Object o) throws IOException {
        final var json = (JSONObject) o;
        final var worldId = fromJson(UUID.class, json.get("worldId"));
        final var x = fromJson(double.class, json.get("x"));
        final var y = fromJson(double.class, json.get("y"));
        final var z = fromJson(double.class, json.get("z"));
        final var pitch = fromJson(float.class, json.get("pitch"));
        final var yaw = fromJson(float.class, json.get("yaw"));
        return new LazyLocation(worldId, x, y, z, yaw, pitch);
    }

    private static Object serializeLazyBlock(@NotNull final Object o) throws IOException {
        final var lazyBlock = (LazyBlock) o;
        final var json = new JSONObject();
        json.put("worldId", toJson(UUID.class, lazyBlock.worldId()));
        json.put("x", toJson(int.class, lazyBlock.x()));
        json.put("y", toJson(int.class, lazyBlock.y()));
        json.put("z", toJson(int.class, lazyBlock.z()));
        return json;
    }

    private static LazyBlock deserializeLazyBlock(@NotNull final Object o) throws IOException {
        final var json = (JSONObject) o;
        final var worldId = fromJson(UUID.class, json.get("worldId"));
        final var x = fromJson(int.class, json.get("x"));
        final var y = fromJson(int.class, json.get("y"));
        final var z = fromJson(int.class, json.get("z"));
        return new LazyBlock(worldId, x, y, z);
    }

    private static Object serializeMaterial(@NotNull final Object o) throws IOException {
        return toJson(NamespacedKey.class, ((Material) o).getKey());
    }

    private static Material deserializeMaterial(@NotNull final Object o) throws IOException {
        return materialFrom(fromJson(NamespacedKey.class, o));
    }

    private static Object serializeItemStack(@NotNull final Object o) throws IOException {
        return new String(Base64.getEncoder().encode(((ItemStack) o).serializeAsBytes()), StandardCharsets.UTF_8);
    }

    private static ItemStack deserializeItemStack(@NotNull final Object o) throws IOException {
        return ItemStack.deserializeBytes(Base64.getDecoder().decode(((String) o).getBytes(StandardCharsets.UTF_8)));
    }

    private static boolean isNull(Object o) {
        return o == null || o == JSONObject.NULL;
    }

    public static final Map<Class<?>, Function<Object, Object>> serializers = new HashMap<>();
    public static final Map<Class<?>, Function<Object, Object>> deserializers = new HashMap<>();

    static {
        // Primitive types
        serializers.put(boolean.class, String::valueOf);
        serializers.put(char.class, String::valueOf);
        serializers.put(double.class, String::valueOf);
        serializers.put(float.class, String::valueOf);
        serializers.put(int.class, String::valueOf);
        serializers.put(long.class, String::valueOf);
        serializers.put(Boolean.class, String::valueOf);
        serializers.put(Character.class, String::valueOf);
        serializers.put(Double.class, String::valueOf);
        serializers.put(Float.class, String::valueOf);
        serializers.put(Integer.class, String::valueOf);
        serializers.put(Long.class, String::valueOf);

        deserializers.put(boolean.class, x -> Boolean.parseBoolean((String) x));
        deserializers.put(char.class, x -> ((String) x).charAt(0));
        deserializers.put(double.class, x -> Double.parseDouble((String) x));
        deserializers.put(float.class, x -> Float.parseFloat((String) x));
        deserializers.put(int.class, x -> Integer.parseInt((String) x));
        deserializers.put(long.class, x -> Long.parseLong((String) x));
        deserializers.put(Boolean.class, x -> Boolean.valueOf((String) x));
        deserializers.put(Character.class, x -> ((String) x).charAt(0));
        deserializers.put(Double.class, x -> Double.valueOf((String) x));
        deserializers.put(Float.class, x -> Float.valueOf((String) x));
        deserializers.put(Integer.class, x -> Integer.valueOf((String) x));
        deserializers.put(Long.class, x -> Long.valueOf((String) x));

        // Other types
        serializers.put(String.class, x -> x);
        deserializers.put(String.class, x -> x);
        serializers.put(UUID.class, Object::toString);
        deserializers.put(UUID.class, x -> UUID.fromString((String) x));

        // Bukkit types
        serializers.put(NamespacedKey.class, PersistentSerializer::serializeNamespacedKey);
        deserializers.put(NamespacedKey.class, PersistentSerializer::deserializeNamespacedKey);
        serializers.put(LazyLocation.class, PersistentSerializer::serializeLazyLocation);
        deserializers.put(LazyLocation.class, PersistentSerializer::deserializeLazyLocation);
        serializers.put(LazyBlock.class, PersistentSerializer::serializeLazyBlock);
        deserializers.put(LazyBlock.class, PersistentSerializer::deserializeLazyBlock);
        serializers.put(Material.class, PersistentSerializer::serializeMaterial);
        deserializers.put(Material.class, PersistentSerializer::deserializeMaterial);
        serializers.put(ItemStack.class, PersistentSerializer::serializeItemStack);
        deserializers.put(ItemStack.class, PersistentSerializer::deserializeItemStack);
    }

    public static Object toJson(final Field field, final Object value) throws IOException {
        return toJson(field.getGenericType(), value);
    }

    public static Object toJson(final Class<?> cls, final Object value) throws IOException {
        final var serializer = serializers.get(cls);
        if (serializer == null) {
            throw new IOException("Cannot serialize " + cls + ". This is a bug.");
        }
        if (isNull(value)) {
            return JSONObject.NULL;
        }
        return serializer.apply(value);
    }

    public static Object toJson(final Type type, final Object value) throws IOException {
        if (type instanceof ParameterizedType) {
            final var parameterizedType = (ParameterizedType) type;
            final var baseType = parameterizedType.getRawType();
            final var typeArgs = parameterizedType.getActualTypeArguments();
            if (baseType.equals(Map.class)) {
                final var K = (Class<?>) typeArgs[0];
                final var V = typeArgs[1];
                final var json = new JSONObject();
                for (final var e : ((Map<?, ?>) value).entrySet()) {
                    json.put((String) toJson(K, e.getKey()), toJson(V, e.getValue()));
                }
                return json;
            } else if (baseType.equals(Set.class)) {
                final var T = typeArgs[0];
                final var json = new JSONArray();
                for (final var t : (Set<?>) value) {
                    json.put(toJson(T, t));
                }
                return json;
            } else if (baseType.equals(List.class)) {
                final var T = typeArgs[0];
                final var json = new JSONArray();
                for (final var t : (List<?>) value) {
                    json.put(toJson(T, t));
                }
                return json;
            } else {
                throw new IOException("Cannot serialize " + type + ". This is a bug.");
            }
        } else {
            return toJson((Class<?>) type, value);
        }
    }

    public static Object fromJson(final Field field, final Object value) throws IOException {
        return fromJson(field.getGenericType(), value);
    }

    @SuppressWarnings("unchecked")
    public static <U> U fromJson(final Class<U> cls, final Object value) throws IOException {
        final var deserializer = deserializers.get(cls);
        if (deserializer == null) {
            throw new IOException("Cannot deserialize " + cls + ". This is a bug.");
        }
        if (isNull(value)) {
            return null;
        }
        return (U) deserializer.apply(value);
    }

    public static Object fromJson(final Type type, final Object json) throws IOException {
        if (type instanceof ParameterizedType) {
            final var parameterizedType = (ParameterizedType) type;
            final var baseType = parameterizedType.getRawType();
            final var typeArgs = parameterizedType.getActualTypeArguments();
            if (baseType.equals(Map.class)) {
                final var K = (Class<?>) typeArgs[0];
                final var V = typeArgs[1];
                final var value = new HashMap<Object, Object>();
                for (final var key : ((JSONObject) json).keySet()) {
                    value.put(fromJson(K, key), fromJson(V, ((JSONObject) json).get(key)));
                }
                return value;
            } else if (baseType.equals(Set.class)) {
                final var T = typeArgs[0];
                final var value = new HashSet<Object>();
                for (final var t : (JSONArray) json) {
                    value.add(fromJson(T, t));
                }
                return value;
            } else if (baseType.equals(List.class)) {
                final var T = typeArgs[0];
                final var value = new ArrayList<Object>();
                for (final var t : (JSONArray) json) {
                    value.add(fromJson(T, t));
                }
                return value;
            } else {
                throw new IOException("Cannot deserialize " + type + ". This is a bug.");
            }
        } else {
            return fromJson((Class<?>) type, json);
        }
    }
}
