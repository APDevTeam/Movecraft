package net.countercraft.movecraft.craft.datatag;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.util.registration.SimpleRegistry;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class CraftDataTagRegistry extends SimpleRegistry<NamespacedKey, CraftDataTagKey<?>> {
    public static final @NotNull CraftDataTagRegistry INSTANCE = new CraftDataTagRegistry();

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
        return (CraftDataTagKey<T>) super.register(key, result, false);
    }

    @Override
    public @NotNull CraftDataTagKey<?> register(@NotNull NamespacedKey key, @NotNull CraftDataTagKey<?> value, boolean override) throws IllegalArgumentException {
        return super.register(key, value, false);
    }

    /**
     * Get an iterable over all keys currently registered.
     * @return An immutable iterable over the registry keys
     */
    public @NotNull Iterable<@NotNull NamespacedKey> getAllKeys(){
        return _register.keySet().stream().toList();
    }
}
