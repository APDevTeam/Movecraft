package net.countercraft.movecraft.craft.datatag;

import net.countercraft.movecraft.craft.Craft;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public class CraftDataTagRegistry {
    private final @NotNull ConcurrentMap<@NotNull NamespacedKey, @NotNull CraftDataTagKey<?>> _registeredTags;

    public CraftDataTagRegistry(){
        _registeredTags = new ConcurrentHashMap<>();
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
    public <T> @NotNull CraftDataTagKey<T> registerTagKey(final @NotNull NamespacedKey key, final @NotNull Function<Craft, T> initializer) throws IllegalArgumentException {
        CraftDataTagKey<T> result = new CraftDataTagKey<>(key, initializer);
        var previous = _registeredTags.putIfAbsent(key, result);
        if(previous != null){
            throw new IllegalArgumentException(String.format("Key %s is already registered.", key));
        }

        return result;
    }

    public boolean isRegistered(final @NotNull NamespacedKey key){
        return _registeredTags.containsKey(key);
    }
}
