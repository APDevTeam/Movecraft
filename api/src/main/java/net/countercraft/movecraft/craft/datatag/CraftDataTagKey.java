package net.countercraft.movecraft.craft.datatag;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.type.TypeSafeCraftType;
import net.countercraft.movecraft.util.registration.TypedKey;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class CraftDataTagKey<T> extends TypedKey<T> {

    protected final Function<Craft, T> dataCreator;

    CraftDataTagKey(@NotNull Plugin plugin, @NotNull String key, @NotNull Function<Craft, T> supplier) {
        super(plugin, key);
        this.dataCreator = supplier;
    }

    CraftDataTagKey(@NotNull String namespace, @NotNull String key, @NotNull Function<Craft, T> supplier) {
        super(namespace, key);
        this.dataCreator = supplier;
    }

    CraftDataTagKey(@NotNull NamespacedKey key, @NotNull Function<Craft, T> supplier) {
        super(key);
        this.dataCreator = supplier;
    }

    public T createNew(final Craft craft) {
        return this.dataCreator.apply(craft);
    }
}
