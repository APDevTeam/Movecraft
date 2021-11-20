package net.countercraft.movecraft.craft.type.property;

import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.craft.type.TypeData;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Property<Type> {
    @Nullable
    Type load(@NotNull TypeData data, @NotNull CraftType type);

    @NotNull
    String getFileKey();

    @NotNull
    NamespacedKey getNamespacedKey();
}
