package net.countercraft.movecraft.processing.effects;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.mapUpdater.update.BlockCreateCommand;
import net.countercraft.movecraft.processing.MovecraftWorld;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

/**
 * Sets a block based on the provided data.
 */
public final class SetBlockEffect implements Effect {
    private final @NotNull MovecraftWorld world;
    private final @NotNull MovecraftLocation location;
    private final @NotNull BlockData data;

    public SetBlockEffect(@NotNull MovecraftWorld world, @NotNull MovecraftLocation location, @NotNull BlockData data){
        this.world = world;
        this.location = location;
        this.data = data;
    }

    @Override
    public void run() {
        World bukkitWorld = Bukkit.getWorld(world.getWorldUUID());
        if(bukkitWorld == null) {
            return;
        }
        // TODO: Reverse indirection
        new BlockCreateCommand(bukkitWorld, location, data).doUpdate();
    }
}
