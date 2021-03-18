package net.countercraft.movecraft.processing;

import net.countercraft.movecraft.MovecraftLocation;
import org.bukkit.block.BlockState;

public class ChunkSectionSnapshot {
    private final BlockState[] states;

    public ChunkSectionSnapshot(BlockState[] states) {
        this.states = states;
    }

    public BlockState getState(MovecraftLocation location){
        return this.getState(location.getX(), location.getY(), location.getZ());
    }

    public BlockState getState(int x, int y, int z){
        if( x < 0 || x > 15 || y < 0 || y > 15 || z < 0 || z > 15){
            throw new IndexOutOfBoundsException("Coordinates must be in range 0-15, found <" + x + ", " + y + ", " + z + ">");
        }
        return this.states[x + 16 * y + 16 * 16 * z];
    }
}
