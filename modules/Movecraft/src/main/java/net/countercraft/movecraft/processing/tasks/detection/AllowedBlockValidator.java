package net.countercraft.movecraft.processing.tasks.detection;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.CraftType;
import net.countercraft.movecraft.processing.MovecraftWorld;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AllowedBlockValidator implements DetectionValidator{
    @Override
    public Modifier validate(@NotNull MovecraftLocation location, @NotNull CraftType type, @NotNull MovecraftWorld world, @Nullable Player player) {
        return type.getAllowedBlocks().contains(world.getMaterial(location)) ? Modifier.PERMIT : Modifier.NONE;
    }
}
