package net.countercraft.movecraft;

import net.countercraft.movecraft.craft.Craft;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public abstract class WorldHandler {
    public abstract void rotateCraft(@NotNull Craft craft, @NotNull MovecraftLocation originLocation, @NotNull Rotation rotation);
    public abstract void translateCraft(@NotNull Craft craft, @NotNull MovecraftLocation newLocation, @NotNull World world);
    public abstract void setBlockFast(@NotNull Location location, @NotNull BlockData data);
    public abstract void setBlockFast(@NotNull Location location, @NotNull Rotation rotation, @NotNull BlockData data);
    public abstract void disableShadow(@NotNull Material type);
    public void addPlayerLocation(Player player, double x, double y, double z, float yaw, float pitch){
        Location playerLoc = player.getLocation();
        player.teleport(new Location(player.getWorld(), x + playerLoc.getX(),y + playerLoc.getY(),z + playerLoc.getZ(),yaw + playerLoc.getYaw(),pitch + playerLoc.getPitch()));
    }
}
