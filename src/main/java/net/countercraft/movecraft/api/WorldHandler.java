package net.countercraft.movecraft.api;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.utils.MovecraftLocation;
import net.countercraft.movecraft.utils.Rotation;
import org.jetbrains.annotations.NotNull;

public abstract class WorldHandler {
    public abstract void translateCraft(@NotNull Craft craft, @NotNull MovecraftLocation newLocation, @NotNull Rotation rotation);
    public abstract void translateCraft(@NotNull Craft craft, @NotNull MovecraftLocation newLocation);
}
