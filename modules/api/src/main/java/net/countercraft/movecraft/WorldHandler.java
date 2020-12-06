package net.countercraft.movecraft;

import net.countercraft.movecraft.craft.Craft;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public abstract class WorldHandler {
    public abstract void rotateCraft(@NotNull Craft craft, @NotNull MovecraftLocation originLocation, @NotNull Rotation rotation);
    public abstract void setBlockFast(@NotNull Location location, @NotNull Material material, Object data);
    public abstract void setBlockFast(@NotNull Location location, @NotNull Rotation rotation, @NotNull Material material, Object data);
    public abstract void translateCraft(@NotNull Craft craft, @NotNull MovecraftLocation newLocation, @NotNull World world);
    public abstract void disableShadow(@NotNull Material type);
    public void addPlayerLocation(Player player, double x, double y, double z, float yaw, float pitch){
        Location playerLoc = player.getLocation();
        Block standingOn = playerLoc.getBlock().getRelative(BlockFace.DOWN);
        Location tpLoc = new Location(player.getWorld(), x + playerLoc.getX(),y + playerLoc.getY(),z + playerLoc.getZ(),yaw + playerLoc.getYaw(),pitch + playerLoc.getPitch());
        player.teleport(tpLoc);
        player.sendBlockChange(tpLoc.subtract(0,1,0), standingOn.getType(), standingOn.getData());
    }

    public void loadChunk(MovecraftChunk chunk) {
        chunk.getWorld().loadChunk(chunk.getX(), chunk.getZ());
    }
}
