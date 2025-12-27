package net.countercraft.movecraft.util.registration;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

public class TypedContainer<K extends TypedKey<?>> {

    private final Map<K, Object> backing;

    public TypedContainer(Supplier<Map<K, Object>> mapCreator) {
        this.backing = mapCreator.get();
    }

    public TypedContainer() {
        this.backing = new ConcurrentHashMap<>();
    }

    protected <T> @Nullable T get(@NotNull TypedKey<T> key, Function<K, T> defaultSupplier) {
        Object stored = this.backing.computeIfAbsent((K) key, defaultSupplier);
        try {
            //noinspection unchecked
            return (T) stored;
        } catch (ClassCastException cce) {
            throw new IllegalStateException(String.format("The provided key %s has an invalid value type.", key), cce);
        }
    }

    protected <T> @Nullable T getOrDefault(@NotNull TypedKey<T> key, T defaultValue) {
        Object stored = this.backing.getOrDefault((K) key, defaultValue);
        try {
            //noinspection unchecked
            return (T) stored;
        } catch (ClassCastException cce) {
            throw new IllegalStateException(String.format("The provided key %s has an invalid value type.", key), cce);
        }
    }

    protected <T> @Nullable T get(@NotNull TypedKey<T> key) {
        Object stored = this.backing.get((K) key);
        if (stored == null) {
            return null;
        }
        try {
            //noinspection unchecked
            return (T) stored;
        } catch (ClassCastException cce) {
            throw new IllegalStateException(String.format("The provided key %s has an invalid value type.", key), cce);
        }
    }

    protected <T> void set(@NotNull TypedKey<T> tagKey, @NotNull T value) {
        this.backing.put((K) tagKey, value);
    }

    protected <T> void delete(@NotNull TypedKey<T> key) {
        this.backing.remove(key);
    }

    public boolean has(@NotNull K tagKey) {
        if (tagKey == null) {
            return false;
        }
        return this.backing.containsKey(tagKey);
    }

    protected Set<Map.Entry<K, Object>> entries() {
        return this.backing.entrySet();
    }

}
