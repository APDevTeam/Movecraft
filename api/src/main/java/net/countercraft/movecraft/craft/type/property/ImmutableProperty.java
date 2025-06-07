package net.countercraft.movecraft.craft.type.property;

import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.craft.type.TypeData;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ImmutableProperty<T> implements Property<T> {

    private final Property<T> property;

    public ImmutableProperty(final Property<T> property) {
        this.property = property;
    }

    public Property<?> getProperty() {
        return this.property;
    }

    @Override
    public @Nullable T load(@NotNull TypeData data, @NotNull CraftType type) {
        return this.property.load(data, type);
    }

    @Override
    public @NotNull String getFileKey() {
        return this.property.getFileKey();
    }

    @Override
    public @NotNull NamespacedKey getNamespacedKey() {
        return this.property.getNamespacedKey();
    }
}
