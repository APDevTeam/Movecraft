package net.countercraft.movecraft;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Chunk;
import org.bukkit.World;

public class MovecraftChunk {
	
	private final int x, z;
	private final World world;
	
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
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof MovecraftChunk) {
			MovecraftChunk c = (MovecraftChunk) o;
			if (c.getX() == x && c.getZ() == z && c.getWorld().equals(world)) {
				return true;
			}
		}
		return false;
	}
	
	
	public static void addSurroundingChunks(List<MovecraftChunk> chunks, int radius) {
		
		if (radius > 0) {
	    	List<MovecraftChunk> tmp = new ArrayList<MovecraftChunk>();
	    	tmp.addAll(chunks);
	    	for (MovecraftChunk chunk : tmp) {
	    		for (int x = -radius; x <= radius; x++) {
	    			for (int z = -radius; z <= radius; z++) {
	    				MovecraftChunk c = new MovecraftChunk(chunk.getX() + x, chunk.getZ() + z, chunk.getWorld());
	    				if (!chunks.contains(c)) chunks.add(c);
	    			}
	    		}
	    	}
    	}
		
	}
	
}
