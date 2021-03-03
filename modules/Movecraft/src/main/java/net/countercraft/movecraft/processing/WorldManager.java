package net.countercraft.movecraft.processing;

import net.countercraft.movecraft.MovecraftLocation;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 */
public class WorldManager extends BukkitRunnable {

    public static final WorldManager INSTANCE = new WorldManager();
    private static final Runnable POISON = () -> {/* No-op */};

    private final ConcurrentLinkedQueue<Runnable> worldChanges = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<ChunkLocation, ChunkSectionSnapshot> cache = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Runnable> tasks = new ConcurrentLinkedQueue<>();
    private final ExecutorService threads = Executors.newCachedThreadPool();
    private final BlockingQueue<Runnable> currentTasks = new LinkedBlockingQueue<>();

    private WorldManager(){}

    private static MovecraftLocation toSectionLocation(MovecraftLocation location){
        return new MovecraftLocation(location.getX() % 16, location.getY() % 16, location.getZ() % 16);
    }

    private static MovecraftLocation toChunkLocation(MovecraftLocation location){
        return new MovecraftLocation(location.getX() / 16, location.getY()/16, location.getZ()/16);
    }

    @Override
    public void run() {
        if(!Bukkit.isPrimaryThread()){
            return;
        }
        Runnable runnable;
        int remaining = tasks.size();
        while(!tasks.isEmpty()){
            currentTasks.add(tasks.poll());
        }
        // process pre-queued tasks and their requests to the main thread
        while(true){
            try {
                runnable = currentTasks.take();
            } catch (InterruptedException e) {
                continue;
            }
            if(runnable == POISON){
                remaining--;
                if(remaining == 0){
                    break;
                }
            }
            threads.execute(runnable);
        }
        // process world updates on the main thread
        while((runnable = worldChanges.poll()) != null){
            runnable.run();
        }
        // Dump cache
        cache.clear();
    }

    public void shutdown(){
        threads.shutdown();
    }

    public Material getMaterial(MovecraftLocation location, World world){
        try {
            return this.getChunkSnapshot(location, world).get().getMaterial(toSectionLocation(location));
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Gets a ChunkSectionSnapshot
     * @param location the wolrd position containing the snapshot
     * @return a snapshot of a section of a chunk
     */
    private Future<ChunkSectionSnapshot> getChunkSnapshot(MovecraftLocation location, World world){
        var task = new FutureTask<>(() -> cache.computeIfAbsent(ChunkLocation.from(location, world), (chunkLocation) -> {
            var chunk = world.getChunkAt(location.toBukkit(world));
            var materials = new Material[16 * 16 * 16];
            for(int x = 0; x < 16; x++){
                for(int y = 0; y < 16; y++){
                    for(int z = 0; z < 16; z++){
                        var state = chunk.getBlock(chunkLocation.getX(), chunkLocation.getY(), chunkLocation.getZ()).getState();
                        materials[ x + 16 * y + 16 * 16 * z] = state.getType();
                    }
                }
            }
            return new ChunkSectionSnapshot(materials);
        }));
        currentTasks.add(task);
        return task;
    }

    private static class ChunkLocation {
        private final MovecraftLocation location;
        private final World world;

        public static ChunkLocation from(MovecraftLocation location, World world){
            return new ChunkLocation(toChunkLocation(location), world);
        }

        private ChunkLocation(MovecraftLocation location, World world){
            this.location = location;
            this.world = world;
        }

        public int getX(){
            return this.location.getX();
        }

        public int getY(){
            return this.location.getY();
        }

        public int getZ(){
            return this.location.getZ();
        }

        @Override
        public int hashCode() {
            return Objects.hash(location, world);
        }

        @Override
        public boolean equals(Object other){
            if(!(other instanceof ChunkLocation)){
                return false;
            }
            var otherLocation = (ChunkLocation) other;
            return this.location.equals(otherLocation.location) && this.world.equals(otherLocation.world);
        }
    }

    public void poison(){
        currentTasks.add(POISON);
    }
}
