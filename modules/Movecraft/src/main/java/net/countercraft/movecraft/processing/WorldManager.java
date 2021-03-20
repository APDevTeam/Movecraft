package net.countercraft.movecraft.processing;

import net.countercraft.movecraft.MovecraftLocation;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 */
public class WorldManager extends BukkitRunnable {

    public static final WorldManager INSTANCE = new WorldManager();
    private static final Runnable POISON = new Runnable() {
        @Override
        public void run() {/* No-op */}
        @Override
        public String toString(){
            return "POISON TASK";
        }
    };

    private final ConcurrentLinkedQueue<Runnable> worldChanges = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<ChunkLocation, ChunkSectionSnapshot> cache = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Runnable> tasks = new ConcurrentLinkedQueue<>();
    private final ExecutorService threads = Executors.newCachedThreadPool();
    private final BlockingQueue<Runnable> currentTasks = new LinkedBlockingQueue<>();

    private WorldManager(){}

    private static MovecraftLocation toSectionLocation(MovecraftLocation location){
        return new MovecraftLocation(location.getX() & 0x0f, location.getY(), location.getZ() & 0x0f);
    }

    @Override
    public void run() {
        if(!Bukkit.isPrimaryThread()){
            throw new RuntimeException("WorldManager must be executed on the main thread.");
        }
        Runnable runnable;
        int remaining = tasks.size();
        if(tasks.isEmpty())
            return;
        while(!tasks.isEmpty()){
            threads.execute(tasks.poll());
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
            runnable.run();
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
        return this.getChunkSnapshot(location, world).getState(toSectionLocation(location)).getBlockData().getMaterial();
    }

    public BlockData getData(MovecraftLocation location, World world){
        return this.getChunkSnapshot(location, world).getState(toSectionLocation(location)).getBlockData();
    }

    public BlockState getState(MovecraftLocation location, World world){
        return this.getChunkSnapshot(location, world).getState(toSectionLocation(location));
    }

    /**
     * Gets a ChunkSectionSnapshot
     * @param location the world position containing the snapshot
     * @return a snapshot of a section of a chunk
     */
    @NotNull
    private ChunkSectionSnapshot getChunkSnapshot(MovecraftLocation location, World world){
        if(cache.containsKey(ChunkLocation.from(location, world))){
            return cache.get(ChunkLocation.from(location, world));
        }
        var task = new FutureTask<>(() -> cache.computeIfAbsent(ChunkLocation.from(location, world), (chunkLocation) -> {
            if (!Bukkit.isPrimaryThread()) {
                throw new RuntimeException("chunk snapshots must be calculated on the main thread.");
            }
            var chunk = world.getChunkAt(location.toBukkit(world));
            var snapshot = chunk.getChunkSnapshot(false, false, false);
            return new ChunkSectionSnapshot(chunk.getTileEntities(), snapshot);
        }));
        currentTasks.add(task);
        try {
            return task.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

    }

    private static class ChunkLocation {
        private final int x;
        private final int z;
        private final World world;

        public static ChunkLocation from(MovecraftLocation location, World world){
            return new ChunkLocation(location.getX()>>4, location.getZ()>>4, world);
        }

        private ChunkLocation(int x, int z, World world){
            this.x = x;
            this.z = z;
            this.world = world;
        }

        public int getX(){
            return this.x;
        }

        public int getZ(){
            return this.z;
        }

        @Override
        public String toString(){
            return String.format("(%d, %d @ %s)", getX(), getZ(), world);
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, z, world);
        }

        @Override
        public boolean equals(Object other){
            if(!(other instanceof ChunkLocation)){
                return false;
            }
            var otherLocation = (ChunkLocation) other;
            return this.x == otherLocation.x &&
                    this.z == otherLocation.z &&
                    this.world.equals(otherLocation.world);
        }
    }

    public void poison(){
        currentTasks.add(POISON);
    }

    public void submit(WorldTask task){
        this.tasks.add(task);
    }
}
