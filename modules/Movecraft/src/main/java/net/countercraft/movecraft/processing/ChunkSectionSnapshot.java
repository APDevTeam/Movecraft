package net.countercraft.movecraft.processing;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.processing.tasks.MovecraftState;
import net.countercraft.movecraft.util.MathUtils;
import org.bukkit.ChunkSnapshot;
import org.bukkit.block.BlockState;

import java.util.HashMap;

public class ChunkSectionSnapshot {
    private final ChunkSnapshot snapshot;
    private final HashMap<MovecraftLocation, BlockState> tiles = new HashMap<>();

    public ChunkSectionSnapshot(BlockState[] tiles, ChunkSnapshot snapshot) {
        this.snapshot = snapshot;
        for(var tile : tiles){
            this.tiles.put(MathUtils.bukkit2MovecraftLoc(tile.getLocation()), tile);
        }
    }

    public BlockState getState(MovecraftLocation location){
        int x = location.getX();
        int y = location.getY();
        int z = location.getZ();
        verifyBounds(x,y,z);
        if(tiles.containsKey(location))
            return tiles.get(location);
        return new MovecraftState(location, snapshot.getBlockData(x, y, z), snapshot.getBlockType(x, y, z));
    }

    private static void verifyBounds(int x, int y, int z){
        if( x < 0 || x > 15 || y < 0 || y > 256 || z < 0 || z > 15){
            throw new IndexOutOfBoundsException("Coordinates must be in range 0-15, found <" + x + ", " + y + ", " + z + ">");
        }
    }
}
