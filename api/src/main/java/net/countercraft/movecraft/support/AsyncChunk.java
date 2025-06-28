package net.countercraft.movecraft.support;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.WorldHandler;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public abstract class AsyncChunk<T extends Chunk> {

    private static final Constructor<?> constructor;
    static {
        Constructor<?> temp = null;
        try {
            final Class<?> clazz = Class.forName("net.countercraft.movecraft.support." + WorldHandler.getPackageName(Bukkit.getServer().getMinecraftVersion()) + ".IAsyncChunk");
            if (AsyncChunk.class.isAssignableFrom(clazz)) {
                temp = clazz.getConstructor(Chunk.class);
            }
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException exception) {
            Bukkit.getLogger().severe(String.format("Error in registering Chunk accessor for version %s from the classpath.", Bukkit.getServer().getMinecraftVersion()));
            exception.printStackTrace();
        }
        constructor = temp;
    }

    @NotNull
    public static AsyncChunk<?> of(@NotNull Chunk chunk){
        try {
            if (constructor == null) throw new RuntimeException();
            return (AsyncChunk<?>) constructor.newInstance(chunk);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull protected final T chunk;

    protected AsyncChunk(@NotNull Chunk chunk) {
        this.chunk = adapt(chunk);
    }

    @NotNull
    protected abstract T adapt(@NotNull Chunk chunk);

    @NotNull
    public abstract Material getType(@NotNull MovecraftLocation location);

    @NotNull
    public abstract BlockData getData(@NotNull MovecraftLocation location);

    @NotNull
    public abstract BlockState getState(@NotNull MovecraftLocation location);
}
