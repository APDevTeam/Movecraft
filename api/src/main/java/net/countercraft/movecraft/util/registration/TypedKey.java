package net.countercraft.movecraft.util.registration;

import net.countercraft.movecraft.craft.datatag.CraftDataTagKey;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class TypedKey<T> {

    protected final NamespacedKey key;

    protected TypedKey(@NotNull Plugin plugin, @NotNull String key) {
        this.key = new NamespacedKey(plugin, key);
    }

    protected TypedKey(@NotNull String namespace, @NotNull String key) {
        this.key = new NamespacedKey(namespace, key);
    }

    protected TypedKey(@NotNull NamespacedKey key) {
        this.key = key;
    }

    public NamespacedKey key() {
        return this.key;
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
}
