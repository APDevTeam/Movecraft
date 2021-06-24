package net.countercraft.movecraft.processing.tasks.detection.validators;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.CraftType;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.processing.functions.Result;
import net.countercraft.movecraft.processing.functions.DetectionPredicate;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AllowedBlockValidator implements DetectionPredicate<MovecraftLocation> {
    @Override
    public Result validate(@NotNull MovecraftLocation location, @NotNull CraftType type, @NotNull MovecraftWorld world, @Nullable CommandSender player) {
        return type.getAllowedBlocks().contains(world.getMaterial(location)) ? Result.succeed() : Result.fail();
    }
}
