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
    private final @NotNull ConcurrentMap<@NotNull CraftDataTagKey<?>, @Nullable Object> _backing;

    public CraftDataTagContainer(){
        _backing = new ConcurrentHashMap<>();
    }

    public static final ConcurrentMap<@NotNull NamespacedKey, @NotNull CraftDataTagKey<?>> REGISTERED_TAGS = new ConcurrentHashMap<>();

    public static <T> @NotNull CraftDataTagKey<T> tryRegisterTagKey(final @NotNull NamespacedKey key, final @NotNull Function<Craft, T> supplier) throws IllegalArgumentException {
        if (REGISTERED_TAGS.containsKey(key)) {
            throw new IllegalArgumentException("Duplicate keys are not allowed!");
        } else {
            CraftDataTagKey<T> result = new CraftDataTagKey<>(key, supplier);
            REGISTERED_TAGS.put(key, result);
            return result;
        }
    }

    public <T> T get(final Craft craft, CraftDataTagKey<T> tagKey) {
        if (!REGISTERED_TAGS.containsKey(tagKey.key)) {
            // TODO: Log error
            return null;
        }
        T result;
        if (!_backing.containsKey(tagKey)) {
            result = tagKey.createNew(craft);
            _backing.put(tagKey, result);
        } else {
            Object stored = _backing.getOrDefault(tagKey, tagKey.createNew(craft));
            try {
                T temp = (T) stored;
                result = temp;
            } catch (ClassCastException cce) {
                // TODO: Log error
                result = tagKey.createNew(craft);
                _backing.put(tagKey, result);
            }
        }
        return result;
    }

    public <T> void set(CraftDataTagKey<T> tagKey, @NotNull T value) {
        _backing.put(tagKey, value);
    }

}
