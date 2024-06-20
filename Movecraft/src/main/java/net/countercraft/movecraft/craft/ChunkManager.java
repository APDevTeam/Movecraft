package net.countercraft.movecraft.craft;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftChunk;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

@Deprecated
public class ChunkManager implements Listener {
    
    private static final Set<MovecraftChunk> chunks = new HashSet<>();
    
    public static void addChunksToLoad(Iterable<MovecraftChunk> list) {
        
        for (MovecraftChunk chunk : list) {
            if (chunks.add(chunk)) {
                if (!chunk.isLoaded()) {
                    chunk.toBukkit().load(true);
                }
            }
            
        }
        
        // remove chunks after 10 seconds
        new BukkitRunnable() {

            @Override
            public void run() {
                ChunkManager.removeChunksToLoad(list);
            }
            
        }.runTaskLaterAsynchronously(Movecraft.getInstance(), 200L);
    }
    
    private static void removeChunksToLoad(Iterable<MovecraftChunk> list) {
        for (MovecraftChunk chunk : list) {
            chunks.remove(chunk);
        }
    }
    
    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        MovecraftChunk c = new MovecraftChunk(chunk.getX(), chunk.getZ(), chunk.getWorld());
//        if (chunks.contains(c)) event.setCancelled(true);
    }
    
    
    public static Set<MovecraftChunk> getChunks(Iterable<MovecraftLocation> hitBox, World world) {
        return getChunks(hitBox, world, 0,0,0);
        
    }
    
    public static Set<MovecraftChunk> getChunks(Iterable<MovecraftLocation> oldHitBox, World world, int dx, int dy, int dz) {
        Set<MovecraftChunk> chunks = new HashSet<>();
        for (MovecraftLocation oldLocation : oldHitBox) {
            var location = oldLocation.translate(dx, dy, dz);
            int chunkX = location.getX() / 16;
            if (location.getX() < 0) chunkX--;
            int chunkZ = location.getZ() / 16;
            if (location.getZ() < 0) chunkZ--;

            MovecraftChunk chunk = new MovecraftChunk(chunkX, chunkZ, world);
            chunks.add(chunk);

        }

        return chunks;
    }
    
    public static void checkChunks(Set<MovecraftChunk> chunks) {
        chunks.removeIf(MovecraftChunk::isLoaded);
        
    }
    
    public static Future<Boolean> syncLoadChunks(Set<MovecraftChunk> chunks) {
        if (Settings.Debug)
            Movecraft.getInstance().getLogger().info("Loading " + chunks.size() + " chunks...");
        if(Bukkit.isPrimaryThread()){
            ChunkManager.addChunksToLoad(chunks);
            return CompletableFuture.completedFuture(true);
        }
        return Bukkit.getScheduler().callSyncMethod(Movecraft.getInstance(), () -> {
            ChunkManager.addChunksToLoad(chunks);
            return true;
        });
    }
    
}
