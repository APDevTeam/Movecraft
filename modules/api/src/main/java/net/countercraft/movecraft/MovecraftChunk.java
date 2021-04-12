package net.countercraft.movecraft;

import com.google.common.primitives.Ints;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class MovecraftChunk implements Comparable<MovecraftChunk>{
    
    private final int x, z;
    private final World world;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MovecraftChunk that = (MovecraftChunk) o;
        return x == that.x && z == that.z && world.equals(that.world);
    }

    @Override
    public int hashCode() {
        return  x + 31* z + 31*31*world.hashCode();
    }

    public MovecraftChunk(int x, int z, World world) {
        this.x = x;
        this.z = z;
        this.world = world;
    }
    
    public int getX() {
        return x;
    }
    
    public int getZ() {
        return z;
    }
    
    public World getWorld() {
        return world;
    }
    
    public boolean isLoaded() {
        return world.isChunkLoaded(x, z);
    }
    
    public Chunk toBukkit() {
        return world.getChunkAt(x, z);
    }

    public static void addSurroundingChunks(Set<MovecraftChunk> chunks, int radius) {
        
        if (radius > 0) {
            List<MovecraftChunk> tmp = new ArrayList<>(chunks);
            for (MovecraftChunk chunk : tmp) {
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        MovecraftChunk c = new MovecraftChunk(chunk.getX() + x, chunk.getZ() + z, chunk.getWorld());
                        chunks.add(c);
                    }
                }
            }
        }
        
    }

    @Override
    public int compareTo(@NotNull MovecraftChunk o) {
        return this.x < o.x ? -1 : this.z < o.z ? -1 : this.world.getUID().compareTo(o.world.getUID());
    }
}
