package net.countercraft.movecraft.api;

import net.countercraft.movecraft.api.craft.Craft;
import org.bukkit.Location;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

public abstract class WorldHandler {
    public abstract void translateCraft(@NotNull Craft craft, @NotNull MovecraftLocation newLocation, @NotNull Rotation rotation);
    public abstract void translateCraft(@NotNull Craft craft, @NotNull MovecraftLocation newLocation);
    public abstract void setBlockFast(@NotNull Location location, @NotNull Material material, byte data);
}
