package net.countercraft.movecraft.processing;

import net.countercraft.movecraft.MovecraftLocation;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class IMovecraftWorld implements MovecraftWorld{

    private final World world;

    public IMovecraftWorld(@NotNull World world){
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

    @Override
    public int hashCode() {
        return world.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof IMovecraftWorld){
            return ((IMovecraftWorld) obj).world.equals(world);
        }
        return false;
    }
}
