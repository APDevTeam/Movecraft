package net.countercraft.movecraft.processing;

import com.google.common.collect.MapMaker;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.support.AsyncChunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

public final class CachedMovecraftWorld implements MovecraftWorld{

    private static final ConcurrentMap<World, MovecraftWorld> WORLDS = new MapMaker().weakKeys().makeMap();

    public static MovecraftWorld of(World world){
        return WORLDS.computeIfAbsent(world, CachedMovecraftWorld::new);
    }

    public static void purge(){
        WORLDS.clear();
    }

    private final ConcurrentHashMap<ChunkLocation, AsyncChunk<?>> chunkCache = new ConcurrentHashMap<>();
    private final World world;
    private final AtomicReference<WorldBorder> border = new AtomicReference<>();

    private CachedMovecraftWorld(@NotNull World world){
        this.world = world;
    }

    @NotNull
    public Material getMaterial(@NotNull MovecraftLocation location){
        var chunkIndex = toSectionLocation(location);
        return this.getChunk(location,world).getType(chunkIndex);
    }

    @NotNull
    public BlockData getData(@NotNull MovecraftLocation location){
        var chunkIndex = toSectionLocation(location);
        return this.getChunk(location,world).getData(chunkIndex);
    }

    @NotNull
    public BlockState getState(@NotNull MovecraftLocation location) {
        var chunkIndex = toSectionLocation(location);
        return this.getChunk(location,world).getState(chunkIndex);
    }

    @NotNull
    public UUID getWorldUUID(){
        return world.getUID();
    }

    @NotNull
    @Override
    public String getName() {
        return world.getName();
    }

    @NotNull
    @Override
    public WorldBorder getWorldBorder() {
        WorldBorder query;
        if((query = border.get()) != null) return query;
        return border.updateAndGet( (b) ->{
            if(b != null) return b;
            return WorldManager.INSTANCE.executeMain(world::getWorldBorder);
        });
    }

    @Override
    public int hashCode() {
        return world.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof CachedMovecraftWorld){
            return ((CachedMovecraftWorld) obj).world.equals(world);
        }
        return false;
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
        test = WorldManager.INSTANCE.executeMain(() -> {
            AsyncChunk<?> temp;
            if((temp = chunkCache.get(chunkLocation)) != null) return temp;
            return AsyncChunk.of(world.getChunkAt(location.toBukkit(world)));
        });
        var previous = chunkCache.putIfAbsent(chunkLocation, test);
        return previous == null ? test : previous;
    }

    @NotNull
    private static MovecraftLocation toSectionLocation(MovecraftLocation location){
        return new MovecraftLocation(location.getX() & 0x0f, location.getY(), location.getZ() & 0x0f);
    }

    private static final class ChunkLocation {
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
            int result =  31 + x;
            result = 31 * result + z;
            result = 31 * result + world.hashCode();
            return result;
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
}
