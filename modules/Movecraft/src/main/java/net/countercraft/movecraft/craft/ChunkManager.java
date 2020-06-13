package net.countercraft.movecraft.craft;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.scheduler.BukkitRunnable;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftChunk;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.utils.BitmapHitBox;

public class ChunkManager implements Listener {
    
    private static List<MovecraftChunk> chunks = new ArrayList<MovecraftChunk>();
    
    public static void addChunksToLoad(List<MovecraftChunk> list) {
        
        for (MovecraftChunk chunk : list) {
            if (!chunks.contains(chunk)) {
                chunks.add(chunk);
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
    
    private static void removeChunksToLoad(List<MovecraftChunk> list) {
        for (MovecraftChunk chunk : list) {
            chunks.remove(chunk);
        }
    }
    
    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        MovecraftChunk c = new MovecraftChunk(chunk.getX(), chunk.getZ(), chunk.getWorld());
        if (chunks.contains(c)) event.setCancelled(true);
    }
    
    
    public static List<MovecraftChunk> getChunks(BitmapHitBox hitBox, World world) {
        
        List<MovecraftChunk> chunks = new ArrayList<MovecraftChunk>();
        
        for (MovecraftLocation location : hitBox) {
            
            int chunkX = location.getX() / 16;
            if (location.getX() < 0) chunkX--;
            int chunkZ = location.getZ() / 16;
            if (location.getZ() < 0) chunkZ--;
            
            MovecraftChunk chunk = new MovecraftChunk(chunkX, chunkZ, world);
            if (!chunks.contains(chunk)) chunks.add(chunk);
            
        }
        
        return chunks;
        
    }
    
    public static List<MovecraftChunk> getChunks(BitmapHitBox oldHitBox, World world, int dx, int dy, int dz) {
        
        BitmapHitBox hitBox = new BitmapHitBox();
        for (MovecraftLocation location : oldHitBox) {
            hitBox.add(location.translate(dx, dy, dz));
        }
        
        return getChunks(hitBox, world);
        
    }
    
    public static void checkChunks(List<MovecraftChunk> chunks) {
        
        List<MovecraftChunk> list = new ArrayList<MovecraftChunk>();
        list.addAll(chunks);
        for (MovecraftChunk chunk : list) {
            if (chunk.isLoaded()) chunks.remove(chunk);
        }
        
    }
    
    public static Future<Boolean> syncLoadChunks(List<MovecraftChunk> chunks) {
        if (Settings.Debug)
            Movecraft.getInstance().getLogger().info("Loading " + chunks.size() + " chunks...");
        
        return Bukkit.getScheduler().callSyncMethod(Movecraft.getInstance(), new Callable<Boolean>() {

            @Override
            public Boolean call() throws Exception {
                ChunkManager.addChunksToLoad(chunks);
                return true;
            }
            
        });
    }
    
}
