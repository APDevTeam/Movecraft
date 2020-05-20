package net.countercraft.movecraft.craft;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.scheduler.BukkitRunnable;

import net.countercraft.movecraft.Movecraft;

public class ChunkManager implements Listener {
	
	private static List<Chunk> allChunks = new ArrayList<Chunk>();
	
	public static void addChunksToLoad(List<Chunk> chunks) {
		List<Chunk> list = new ArrayList<Chunk>();
		for (Chunk chunk : chunks) {
			if (!allChunks.contains(chunk))
				list.add(chunk);
		}
		
		for (Chunk chunk : list) {
			allChunks.add(chunk);
			if (!chunk.isLoaded()) chunk.load(true);
		}
		
		// remove chunks after 10 seconds
		new BukkitRunnable() {

			@Override
			public void run() {
				ChunkManager.removeChunksToLoad(list);
			}
			
		}.runTaskLaterAsynchronously(Movecraft.getInstance(), 200L);
	}
	
	private static void removeChunksToLoad(List<Chunk> chunks) {
		for (Chunk chunk : chunks) {
			allChunks.remove(chunk);
		}
	}
	
	@EventHandler
	public void onChunkUnload(ChunkUnloadEvent event) {
		if (allChunks.contains(event.getChunk())) event.setCancelled(true);
	}
	
}