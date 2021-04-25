package net.countercraft.movecraft.processing;

import net.countercraft.movecraft.MovecraftLocation;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class MovecraftWorld {

    private final World world;

    public MovecraftWorld(@NotNull World world){
        this.world = world;
    }

    @NotNull
    public Material getMaterial(@NotNull MovecraftLocation location){
        return WorldManager.INSTANCE.getMaterial(location, world);
    }

    @NotNull
    public BlockData getData(@NotNull MovecraftLocation location){
        return WorldManager.INSTANCE.getData(location, world);
    }

    @NotNull
    public BlockState getState(@NotNull MovecraftLocation location) {
        return WorldManager.INSTANCE.getState(location, world);
    }

    @NotNull
    public UUID getWorldUUID(){
        return world.getUID();
    }
}
