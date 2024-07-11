package net.countercraft.movecraft.processing;

import net.countercraft.movecraft.MovecraftLocation;
import org.bukkit.Material;
import org.bukkit.WorldBorder;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface MovecraftWorld {
    @NotNull
    Material getMaterial(@NotNull MovecraftLocation location);

    @NotNull
    BlockData getData(@NotNull MovecraftLocation location);

    @NotNull
    BlockState getState(@NotNull MovecraftLocation location);

    @NotNull
    UUID getWorldUUID();

    @NotNull
    String getName();

    @NotNull
    WorldBorder getWorldBorder();
}
