package net.countercraft.movecraft.processing;

import net.countercraft.movecraft.MovecraftLocation;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;

public class MovecraftWorld {

    private final World world;

    public MovecraftWorld(World world){
        this.world = world;
    }

    public Material getMaterial(MovecraftLocation location){
        return WorldManager.INSTANCE.getMaterial(location, world);
    }

    public BlockData getData(MovecraftLocation location){
        return WorldManager.INSTANCE.getData(location, world);
    }

    public BlockState getState(MovecraftLocation location) {
        return WorldManager.INSTANCE.getState(location, world);
    }
}
