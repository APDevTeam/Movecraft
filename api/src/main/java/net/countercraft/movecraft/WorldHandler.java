package net.countercraft.movecraft;

import net.countercraft.movecraft.craft.Craft;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class WorldHandler {
    public abstract void rotateCraft(@NotNull Craft craft, @NotNull MovecraftLocation originLocation, @NotNull MovecraftRotation rotation);
    public abstract void translateCraft(@NotNull Craft craft, @NotNull MovecraftLocation newLocation, @NotNull World world);
    public abstract void setBlockFast(@NotNull Location location, @NotNull BlockData data);
    public abstract void setBlockFast(@NotNull Location location, @NotNull MovecraftRotation rotation, @NotNull BlockData data);
    public abstract @Nullable Location getAccessLocation(@NotNull InventoryView inventoryView);
    public abstract void setAccessLocation(@NotNull InventoryView inventoryView, @NotNull Location location);

    public static @NotNull String getPackageName(@NotNull String minecraftVersion) {
        return "v1_" + minecraftVersion.substring(minecraftVersion.indexOf('.') + 1, minecraftVersion.lastIndexOf('.'));
    }
}
