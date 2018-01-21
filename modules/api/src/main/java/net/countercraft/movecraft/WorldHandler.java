package net.countercraft.movecraft;

import net.countercraft.movecraft.craft.Craft;
import org.bukkit.Location;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

public abstract class WorldHandler {
    public abstract void rotateCraft(@NotNull Craft craft, @NotNull MovecraftLocation originLocation, @NotNull Rotation rotation);
    public abstract void translateCraft(@NotNull Craft craft, @NotNull MovecraftLocation newLocation);
    public abstract void setBlockFast(@NotNull Location location, @NotNull Material material, byte data);
    public abstract void setBlockFast(@NotNull Location location, @NotNull Rotation rotation, @NotNull Material material, byte data);
    public abstract void disableShadow(@NotNull Material type);
}
