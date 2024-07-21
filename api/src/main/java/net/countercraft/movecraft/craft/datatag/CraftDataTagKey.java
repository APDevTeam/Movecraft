package net.countercraft.movecraft.craft.datatag;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Supplier;

public class CraftDataTagKey<T extends ICraftDataTag> {
    protected final Supplier<T> dataCreator;
    protected final NamespacedKey key;

    CraftDataTagKey(@NotNull Plugin plugin, @NotNull String key, @NotNull Supplier<T> supplier) {
        this.key = new NamespacedKey(plugin, key);
        this.dataCreator = supplier;
    }

    CraftDataTagKey(@NotNull String namespace, @NotNull String key, @NotNull Supplier<T> supplier) {
        this.key = new NamespacedKey(namespace, key);
        this.dataCreator = supplier;
    }

    CraftDataTagKey(@NotNull NamespacedKey key, @NotNull Supplier<T> supplier) {
        this.dataCreator = supplier;
        this.key = key;
    }

    @Override
    public int hashCode() {
        return this.key.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CraftDataTagKey<?> other = (CraftDataTagKey) obj;
        return this.key.equals(other.key);
    }

    @Override
    public String toString() {
        return this.key.toString();
    }

    public T createNew() {
        return this.dataCreator.get();
    }
}
