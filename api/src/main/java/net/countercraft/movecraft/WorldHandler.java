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

    public static @NotNull String getPackageName(@NotNull String minecraftVersion) {
        String[] parts = minecraftVersion.split("\\.");
        if (parts.length == 2) {
            return "v" + parts[0] + "_" + parts[1];
        }
        else if (parts.length == 3) {
            return "v" + parts[0] + "_" + parts[1] + "_" + parts[2];
        }
        else {
            throw new IllegalArgumentException();
        }
    }
}
