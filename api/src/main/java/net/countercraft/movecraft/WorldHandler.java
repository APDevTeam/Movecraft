package net.countercraft.movecraft;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.util.AffineTransformation;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class WorldHandler {
    @Deprecated(forRemoval = true)
    public void rotateCraft(@NotNull Craft craft, @NotNull MovecraftLocation originLocation, @NotNull MovecraftRotation rotation){
        transformHitBox(
            craft.getHitBox(),
            AffineTransformation.of(originLocation)
                .mult(AffineTransformation.of(rotation))
                .mult(AffineTransformation.of(originLocation.scalarMultiply(-1))),
            craft.getWorld(),
            craft.getWorld());
    }

    @Deprecated(forRemoval = true)
    public void translateCraft(@NotNull Craft craft, @NotNull MovecraftLocation displacement, @NotNull World world){
        transformHitBox(craft.getHitBox(), AffineTransformation.of(displacement), craft.getWorld(), world);
    }
    public abstract void transformHitBox(@NotNull HitBox hitbox, @NotNull AffineTransformation transformation, @NotNull World originWorld, @NotNull World destinationWorld);
    public abstract void setBlockFast(@NotNull Location location, @NotNull BlockData data);
    public abstract void setBlockFast(@NotNull Location location, @NotNull MovecraftRotation rotation, @NotNull BlockData data);
    @Deprecated(forRemoval = true)
    public @Nullable Location getAccessLocation(@NotNull InventoryView inventoryView){
        // Not needed for 1.20+, remove when dropping support for 1.18.2
        return null;
    }
    @Deprecated(forRemoval = true)
    public void setAccessLocation(@NotNull InventoryView inventoryView, @NotNull Location location){
        // Not needed for 1.20+, remove when dropping support for 1.18.2
    }
    public static @NotNull String getPackageName(@NotNull String minecraftVersion) {
        String[] parts = minecraftVersion.split("\\.");
        if (parts.length < 2)
            throw new IllegalArgumentException();
        return "v1_" + parts[1];
    }
}
