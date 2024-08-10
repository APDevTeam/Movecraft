package net.countercraft.movecraft.craft.datatag;

import net.countercraft.movecraft.craft.Craft;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public class CraftDataTagContainer {
    private static final @NotNull ConcurrentMap<@NotNull NamespacedKey, @NotNull CraftDataTagKey<?>> REGISTERED_TAGS = new ConcurrentHashMap<>();
    private final @NotNull ConcurrentMap<@NotNull CraftDataTagKey<?>, @Nullable Object> _backing;

    public CraftDataTagContainer(){
        _backing = new ConcurrentHashMap<>();
    }

    /**
     * Registers a data tag to be attached to craft instances. The data tag will initialize to the value supplied by the
     * initializer. Once a tag is registered, it can be accessed from crafts using the returned key through various
     * methods.
     *
     * @param key the namespace key to use for registration, which must be unique
     * @param initializer a default initializer for the value type
     * @return A CraftDataTagKey, which can be used to control an associated value on a Craft instance
     * @param <T> the value type
     * @throws IllegalArgumentException when the provided key is already registered
     */
    public static <T> @NotNull CraftDataTagKey<T> registerTagKey(final @NotNull NamespacedKey key, final @NotNull Function<Craft, T> initializer) throws IllegalArgumentException {
        CraftDataTagKey<T> result = new CraftDataTagKey<>(key, initializer);
        var previous = REGISTERED_TAGS.putIfAbsent(key, result);
        if(previous != null){
            throw new IllegalArgumentException(String.format("Key %s is already registered.", key));
        }

        return result;
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

    /**
     * Set the value associated with the provided tagKey on the associated craft.
     *
     * @param tagKey the tagKey to use for storing the relevant data
     * @param value the value to set for future lookups
     * @param <T> the type of the value
     */
    public <T> void set(@NotNull CraftDataTagKey<T> tagKey, @NotNull T value) {
        _backing.put(tagKey, value);
    }
}
