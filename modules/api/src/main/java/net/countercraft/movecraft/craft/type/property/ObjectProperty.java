package net.countercraft.movecraft.craft.type.property;

import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.craft.type.TypeData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ObjectProperty extends Property<Object> {
    @Nullable
    Object load(@NotNull TypeData data, @NotNull CraftType type);

    @NotNull
    String getFileKey();
}
