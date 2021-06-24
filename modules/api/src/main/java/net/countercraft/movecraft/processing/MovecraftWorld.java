package net.countercraft.movecraft.processing;

import net.countercraft.movecraft.MovecraftLocation;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface MovecraftWorld {
    @NotNull
    public Material getMaterial(@NotNull MovecraftLocation location);

    @NotNull
    public BlockData getData(@NotNull MovecraftLocation location);

    @NotNull
    public BlockState getState(@NotNull MovecraftLocation location);

    @NotNull
    public UUID getWorldUUID();

    @NotNull
    public String getName();
}
