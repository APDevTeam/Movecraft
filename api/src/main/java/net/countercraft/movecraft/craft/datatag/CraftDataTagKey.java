package net.countercraft.movecraft.craft.datatag;

import net.countercraft.movecraft.craft.Craft;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class CraftDataTagKey<T> {
    protected final Function<Craft, T> dataCreator;
    protected final NamespacedKey key;

    CraftDataTagKey(@NotNull Plugin plugin, @NotNull String key, @NotNull Function<Craft, T> supplier) {
        this.key = new NamespacedKey(plugin, key);
        this.dataCreator = supplier;
    }

    CraftDataTagKey(@NotNull String namespace, @NotNull String key, @NotNull Function<Craft, T> supplier) {
        this.key = new NamespacedKey(namespace, key);
        this.dataCreator = supplier;
    }

    CraftDataTagKey(@NotNull NamespacedKey key, @NotNull Function<Craft, T> supplier) {
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

    public T createNew(final Craft craft) {
        return this.dataCreator.apply(craft);
    }
}
