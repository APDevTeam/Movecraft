package net.countercraft.movecraft.craft.datatag;

import net.countercraft.movecraft.craft.Craft;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Supplier;

public class CraftDataTagContainer {
    private static final @NotNull ConcurrentMap<@NotNull NamespacedKey, @NotNull CraftDataTagKey<?>> REGISTERED_TAGS = new ConcurrentHashMap<>();
    private final @NotNull ConcurrentMap<@NotNull CraftDataTagKey<?>, @Nullable Object> _backing;

    public CraftDataTagContainer(){
        _backing = new ConcurrentHashMap<>();
    }

    public static <T> @NotNull CraftDataTagKey<T> tryRegisterTagKey(final @NotNull NamespacedKey key, final @NotNull Function<Craft, T> supplier) throws IllegalArgumentException {
        if (REGISTERED_TAGS.containsKey(key)) {
            throw new IllegalArgumentException("Duplicate keys are not allowed!");
        } else {
            CraftDataTagKey<T> result = new CraftDataTagKey<>(key, supplier);
            REGISTERED_TAGS.put(key, result);
            return result;
        }
    }

    /**
     * Gets the data associate with a provided tagKey from a craft.
     * @param craft the craft to perform a lookup against
     * @param tagKey the tagKey to use for looking up the relevant data
     * @return the tag value associate with the provided tagKey on the specified craft
     * @param <T> the value type of the registered data key
     * @throws IllegalArgumentException when the provided tagKey is not registered
     * @throws IllegalStateException when the provided tagKey does not match the underlying tag value
     */
    public <T> T get(final @NotNull Craft craft, CraftDataTagKey<T> tagKey) {
        if (!REGISTERED_TAGS.containsKey(tagKey.key)) {
            throw new IllegalArgumentException(String.format("The provided key %s was not registered.", tagKey));
        }

        Object stored = _backing.computeIfAbsent(tagKey, ignored -> tagKey.createNew(craft));
        try {
            //noinspection unchecked
            return (T) stored;
        } catch (ClassCastException cce) {
            throw new IllegalStateException(String.format("The provided key %s has an invalid value type.", tagKey), cce);
        }
    }

    public <T> void set(CraftDataTagKey<T> tagKey, @NotNull T value) {
        _backing.put(tagKey, value);
    }

}
