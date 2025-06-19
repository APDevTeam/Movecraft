package net.countercraft.movecraft.util.registration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SimpleRegistry<K, T> {

    protected final @NotNull ConcurrentMap<@NotNull K, @NotNull T> _register;

    public SimpleRegistry(){
        _register = new ConcurrentHashMap<>();
    }

    public @NotNull T register(final @NotNull K key, final @NotNull T value) throws IllegalArgumentException {
        return this.register(key, value, false);
    }

    public @NotNull T register(final @NotNull K key, final @NotNull T value, boolean override) throws IllegalArgumentException {
        T previous = _register.put(key, value);
        if (previous != null && !override) {
            _register.put(key, value);
            throw new IllegalArgumentException(String.format("Key %s is already registered.", key));
        }
        return value;
    }

    public @Nullable T get(final @NotNull K key) {
        return _register.getOrDefault(key, null);
    }

    public boolean isRegistered(final @NotNull K key){
        return _register.containsKey(key);
    }

    /**
     * Get an iterable over all keys currently registered.
     * @return An immutable iterable over the registry keys
     */
    public @NotNull Iterable<@NotNull K> getAllKeys(){
        return _register.keySet().stream().toList();
    }

    public Set<Map.Entry<K, T>> entries() {
        return new HashSet<>(this._register.entrySet());
    }

}
