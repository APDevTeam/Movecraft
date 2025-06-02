package net.countercraft.movecraft.craft.datatag;

import net.countercraft.movecraft.craft.Craft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CraftDataTagContainer {
    private final @NotNull ConcurrentMap<@NotNull CraftDataTagKey<?>, @Nullable Object> backing;

    public CraftDataTagContainer(){
        backing = new ConcurrentHashMap<>();
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
        if (!CraftDataTagRegistry.INSTANCE.isRegistered(tagKey.key)) {
            throw new IllegalArgumentException(String.format("The provided key %s was not registered.", tagKey));
        }

        Object stored = backing.computeIfAbsent(tagKey, ignored -> tagKey.createNew(craft));
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
        if (!CraftDataTagRegistry.INSTANCE.isRegistered(tagKey.key)) {
            throw new IllegalArgumentException(String.format("The provided key %s was not registered.", tagKey));
        }

        backing.put(tagKey, value);
    }

    public <T> boolean has(@NotNull CraftDataTagKey<T> tagKey) {
        return backing.containsKey(tagKey);
    }
}
