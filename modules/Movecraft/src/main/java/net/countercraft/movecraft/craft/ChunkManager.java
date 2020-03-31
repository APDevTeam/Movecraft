package net.countercraft.movecraft.craft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.scheduler.BukkitRunnable;

import net.countercraft.movecraft.Movecraft;

public class ChunkManager implements Listener {
	
	private static Map<Integer, List<Chunk>> keepLoaded = new HashMap<Integer, List<Chunk>>();
	private static List<Chunk> allChunks = new ArrayList<Chunk>();
	
	public static void addChunksToLoad(List<Chunk> chunks) {
		int i = 0;
		while (keepLoaded.containsKey(i)) i++;
		final int id = i;
		
		keepLoaded.put(id, chunks);
		for (Chunk chunk : chunks) {
			allChunks.add(chunk);
			if (!chunk.isLoaded()) chunk.load(true);
		}
		
		// remove chunks after 10 seconds
		new BukkitRunnable() {

			@Override
			public void run() {
				ChunkManager.removeChunksToLoad(id);
			}
			
		}.runTaskLaterAsynchronously(Movecraft.getInstance(), 200L);
	}
	
	private static void removeChunksToLoad(int id) {
		for (Chunk chunk : keepLoaded.get(id)) {
			allChunks.remove(chunk);
		}
		keepLoaded.remove(id);
	}
	
	@EventHandler
	public void onChunkUnload(ChunkUnloadEvent event) {
		if (allChunks.contains(event.getChunk())) event.setCancelled(true);
	}
	
}