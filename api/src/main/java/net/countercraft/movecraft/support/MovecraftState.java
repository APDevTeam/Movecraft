package net.countercraft.movecraft.support;

import net.countercraft.movecraft.MovecraftLocation;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.material.MaterialData;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MovecraftState implements BlockState {

    private final MovecraftLocation location;
    private final BlockData data;
    private final Material type;

    public MovecraftState(MovecraftLocation location, BlockData data, Material type){

        this.location = location;
        this.data = data;
        this.type = type;
    }

    @NotNull
    @Override
    public Block getBlock() {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @NotNull
    @Override
    public MaterialData getData() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public BlockData getBlockData() {
        return data;
    }

    @NotNull
    @Override
    public Material getType() {
        return type;
    }

    @Override
    public byte getLightLevel() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public World getWorld() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getX() {
        return location.getX();
    }

    @Override
    public int getY() {
        return location.getY();
    }

    @Override
    public int getZ() {
        return location.getZ();
    }

    @NotNull
    @Override
    public Location getLocation() {
        return location.toBukkit(null);
    }

    @Nullable
    @Override
    public Location getLocation(@Nullable Location location) {
        if(location == null){
            return null;
        }
        location.setX(this.location.getX());
        location.setY(this.location.getY());
        location.setZ(this.location.getZ());
        location.setWorld(null);
        return location;
    }

    @NotNull
    @Override
    public Chunk getChunk() {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public void setData(@NotNull MaterialData materialData) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBlockData(@NotNull BlockData blockData) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setType(@NotNull Material material) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean update() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean update(boolean b) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean update(boolean b, boolean b1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte getRawData() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setRawData(byte b) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isPlaced() {
        return true;
    }

    @Override
    public boolean isCollidable() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setMetadata(@NotNull String s, @NotNull MetadataValue metadataValue) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public List<MetadataValue> getMetadata(@NotNull String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasMetadata(@NotNull String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeMetadata(@NotNull String s, @NotNull Plugin plugin) {
        throw new UnsupportedOperationException();
    }
}
