package net.countercraft.movecraft.craft;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftChunk;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.utils.BitmapHitBox;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class ChunkManager implements Listener {
    
    private static List<MovecraftChunk> chunks = new ArrayList<MovecraftChunk>();

    private static Method SET_CANCELLED_CHUNK_EVENT;

    static {
        try {
            SET_CANCELLED_CHUNK_EVENT = ChunkUnloadEvent.class.getDeclaredMethod("setCancelled", boolean.class);
        } catch (NoSuchMethodException e) {
            SET_CANCELLED_CHUNK_EVENT = null;
        }
    }
    
    public static void addChunksToLoad(List<MovecraftChunk> list) {
        
        for (MovecraftChunk chunk : list) {
            if (!chunks.contains(chunk)) {
                chunks.add(chunk);
                if (!chunk.isLoaded()) {
                    chunk.toBukkit().load(true);
                    chunk.toBukkit().setForceLoaded(true);
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
        Bukkit.getScheduler().callSyncMethod(Movecraft.getInstance(), () -> {
            for (MovecraftChunk chunk : list) {
                chunk.toBukkit().setForceLoaded(false);
            }
            return true;
        });
    }
    
    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        if (!Settings.IsLegacy)
            return;
        Chunk chunk = event.getChunk();
        MovecraftChunk c = new MovecraftChunk(chunk.getX(), chunk.getZ(), chunk.getWorld());
        if (chunks.contains(c)) {
            try {
                SET_CANCELLED_CHUNK_EVENT.invoke(event, true);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
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
