package net.countercraft.movecraft.support.v1_17_R1;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.processing.WorldManager;
import net.countercraft.movecraft.support.AsyncChunk;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.block.state.IBlockData;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_17_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_17_R1.block.data.CraftBlockData;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class IAsyncChunk extends AsyncChunk<CraftChunk> {

    public IAsyncChunk(@NotNull Chunk chunk) {
        super(chunk);
    }

    @NotNull
    @Override
    protected CraftChunk adapt(@NotNull org.bukkit.Chunk chunk) {
        return (CraftChunk) chunk;
    }

    @NotNull
    @Override
    public BlockState getState(@NotNull MovecraftLocation location) {
        var block = chunk.getBlock(location.getX(), location.getY(), location.getZ());
        return WorldManager.INSTANCE.executeMain(block::getState);
    }

    @Override
    @NotNull
    public Material getType(@NotNull MovecraftLocation location){
        return CraftBlockData.fromData(chunk.getHandle().getType(new BlockPosition(location.getX(), location.getY(), location.getZ()))).getMaterial();
    }

    @Override
    @NotNull
    public BlockData getData(@NotNull MovecraftLocation location){
        IBlockData data = chunk.getHandle().getType(new BlockPosition(location.getX(), location.getY(), location.getZ()));
        return CraftBlockData.fromData(data);
    }

}
