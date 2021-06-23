package net.countercraft.movecraft.processing.tasks.detection;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.CraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.processing.functions.Result;
import net.countercraft.movecraft.processing.functions.TaskPredicate;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ForbiddenBlockValidator implements TaskPredicate<MovecraftLocation> {
    @Override
    public Result validate(@NotNull MovecraftLocation location, @NotNull CraftType type, @NotNull MovecraftWorld world, @Nullable CommandSender player) {
        return type.getForbiddenBlocks().contains(world.getMaterial(location)) ? Result.failWithMessage(I18nSupport.getInternationalisedString("Detection - Forbidden block found")) : Result.succeed();
    }
}
