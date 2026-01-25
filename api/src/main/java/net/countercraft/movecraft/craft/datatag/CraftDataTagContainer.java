package net.countercraft.movecraft.craft.datatag;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.util.registration.TypedContainer;
import net.countercraft.movecraft.util.registration.TypedKey;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class CraftDataTagContainer extends TypedContainer<CraftDataTagKey<?>> implements ConfigurationSerializable {
    public CraftDataTagContainer(){
        super();
    }

    @Override
    /**
     * Set the value associated with the provided tagKey on the associated craft.
     *
     * @param tagKey the tagKey to use for storing the relevant data
     * @param value the value to set for future lookups
     * @param <T> the type of the value
     */
    public <T> void set(@NotNull TypedKey<T> tagKey, @NotNull T value) {
        if (!CraftDataTagRegistry.INSTANCE.isRegistered(tagKey.key())) {
            throw new IllegalArgumentException(String.format("The provided key %s was not registered.", tagKey));
        }
        super.set(tagKey, value);
    }

    @Override
    protected <T> @Nullable T get(@NotNull TypedKey<T> key, Function<CraftDataTagKey<?>, T> defaultSupplier) {
        if (!CraftDataTagRegistry.INSTANCE.isRegistered(key.key())) {
            throw new IllegalArgumentException(String.format("The provided key %s was not registered.", key));
        }
        return super.get(key, defaultSupplier);
    }

    /**
     * Gets the data value associated with the provided tagKey from a craft.
     *
     * @param craft the craft to perform a lookup against
     * @param tagKey the tagKey to use for looking up the relevant data
     * @return the tag value associate with the provided tagKey on the specified craft
     * @param <T> the value type of the registered data key
     * @throws IllegalArgumentException when the provided tagKey is not registered
     * @throws IllegalStateException when the provided tagKey does not match the underlying tag value
     */
    public <T> T get(final @NotNull Craft craft, @NotNull CraftDataTagKey<T> tagKey) {
        return this.get(tagKey, (k) -> tagKey.createNew(craft));
    }

    public void remove(@NotNull TypedKey<?> key) {
        if (!CraftDataTagRegistry.INSTANCE.isRegistered(key.key())) {
            throw new IllegalArgumentException(String.format("The provided key %s was not registered.", key));
        }
        super.delete(key);
    }

    public static @NotNull CraftDataTagContainer deserialize(@NotNull Map<String, Object> args) {
        CraftDataTagContainer result = new CraftDataTagContainer();

        for (Map.Entry<String, Object> entry : args.entrySet()) {
            NamespacedKey key = NamespacedKey.fromString(entry.getKey());
            CraftDataTagKey<?> dataTagKey = CraftDataTagRegistry.INSTANCE.get(key);
            if (dataTagKey == null) {
                continue;
            }
            deserializeEntry(dataTagKey, entry.getValue(), result);
        }

        return result;
    }

    protected static <T> void deserializeEntry(CraftDataTagKey<T> key, Object serializedObject, CraftDataTagContainer container) {
        DataTagSerializer<T> deserializer = CraftDataTagRegistry.INSTANCE.getSerializer(key);
        if (deserializer == null) {
            return;
        }

        T deserialized = deserializer.deserialize(serializedObject);
        if (deserialized == null) {
            return;
        }

        container.set(key, deserialized);
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> result = new HashMap<>();

        for (CraftDataTagKey<?> key : this.keySet()) {
            // Special case for normally serializable things
            serializeEntry(key, result);
        }

        return result;
    }

    protected <T> void serializeEntry(CraftDataTagKey<T> key, Map<String, Object> serializationOutput) {
        // If we dont have a value, we can quit early!
        if (!this.has(key)) {
            return;
        }
        T value = this.get(key);
        if (value == null) {
            return;
        }

        DataTagSerializer<T> serializer = CraftDataTagRegistry.INSTANCE.getSerializer(key);
        if (serializer == null) {
            return;
        }

        Object serialized = serializer.serialize(value);
        if (serialized == null) {
            return;
        }

        serializationOutput.put(key.key().toString(), serialized);
    }
}
