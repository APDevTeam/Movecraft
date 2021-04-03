package net.countercraft.movecraft.processing;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.support.AsyncChunk;
import net.countercraft.movecraft.util.CompletableFutureTask;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

/**
 *
 */
public final class WorldManager {

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
    private final ConcurrentHashMap<ChunkLocation, AsyncChunk<?>> chunkCache = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Runnable> tasks = new ConcurrentLinkedQueue<>();
    private final BlockingQueue<Runnable> currentTasks = new LinkedBlockingQueue<>();
    private volatile boolean running = false;

    private WorldManager(){}

    @NotNull
    private static MovecraftLocation toSectionLocation(MovecraftLocation location){
        return new MovecraftLocation(location.getX() & 0x0f, location.getY(), location.getZ() & 0x0f);
    }

    public void run() {
        if(!Bukkit.isPrimaryThread()){
            throw new RuntimeException("WorldManager must be executed on the main thread.");
        }
        running = true;
        Runnable runnable;
        int remaining = tasks.size();
        if(tasks.isEmpty())
            return;
        while(!tasks.isEmpty()){
            CompletableFuture.runAsync(tasks.poll()).whenComplete((a,b) -> poison());
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
        chunkCache.clear();
        running = false;
    }

    @NotNull
    public Material getMaterial(@NotNull MovecraftLocation location, @NotNull World world){
        var chunkIndex = toSectionLocation(location);
        return this.getChunk(location,world).getType(chunkIndex);
    }

    @NotNull
    public BlockData getData(@NotNull MovecraftLocation location, @NotNull World world){
        var chunkIndex = toSectionLocation(location);
        return this.getChunk(location,world).getData(chunkIndex);
    }

    @NotNull
    public BlockState getState(@NotNull MovecraftLocation location, @NotNull World world){
        var chunkIndex = toSectionLocation(location);
        return this.getChunk(location,world).getState(chunkIndex);
    }

    /**
     * Gets an AsyncChunk, loading from the main thread if necessary
     * @param location the world position containing the chunk
     * @return a thread safe accessor of the given chunk
     */
    @NotNull
    private AsyncChunk<?> getChunk(@NotNull MovecraftLocation location, @NotNull World world){
        AsyncChunk<?> test;
        var chunkLocation = ChunkLocation.from(location, world);
        if((test = chunkCache.get(chunkLocation)) != null){
            return test;
        }
        test = executeMain(() -> AsyncChunk.of(world.getChunkAt(location.toBukkit(world))));
        var previous = chunkCache.putIfAbsent(chunkLocation, test);
        return previous == null ? test : previous;
    }

    public <T> T executeMain(Supplier<T> callable){
        if(Bukkit.isPrimaryThread()){
            throw new RuntimeException("Cannot schedule on main thread from the main thread");
        }
        var task = new CompletableFutureTask<>(callable);
        currentTasks.add(task);
        return task.join();
    }

    private static class ChunkLocation {
        private final int x;
        private final int z;
        private final World world;

        @NotNull
        public static ChunkLocation from(@NotNull MovecraftLocation location, @NotNull World world){
            return new ChunkLocation(location.getX()>>4, location.getZ()>>4, world);
        }

        private ChunkLocation(int x, int z, @NotNull World world){
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

        @NotNull
        @Override
        public String toString(){
            return String.format("(%d, %d @ %s)", getX(), getZ(), world);
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, z, world);
        }

        @Override
        public boolean equals(@Nullable Object other){
            if(!(other instanceof ChunkLocation)){
                return false;
            }
            var otherLocation = (ChunkLocation) other;
            return this.x == otherLocation.x &&
                    this.z == otherLocation.z &&
                    this.world.equals(otherLocation.world);
        }
    }

    private void poison(){
        currentTasks.add(POISON);
    }

    public void submit(Runnable task){
        tasks.add(task);
    }

    public boolean isRunning() {
        return running;
    }
}
