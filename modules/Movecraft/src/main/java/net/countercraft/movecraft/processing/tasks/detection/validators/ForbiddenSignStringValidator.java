package net.countercraft.movecraft.processing.tasks.detection.validators;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.CraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.processing.functions.Result;
import net.countercraft.movecraft.processing.functions.TaskPredicate;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ForbiddenSignStringValidator implements TaskPredicate<MovecraftLocation> {
    @Override
    public Result validate(@NotNull MovecraftLocation location, @NotNull CraftType type, @NotNull MovecraftWorld world, @Nullable CommandSender player) {
        BlockState state = world.getState(location);
        if (!(state instanceof Sign)) {
            return Result.succeed();
        }
        Sign sign = (Sign) state;
        for(var line : sign.getLines()){
            if(type.getForbiddenSignStrings().contains(line.toLowerCase())){
                return Result.failWithMessage(I18nSupport.getInternationalisedString(
                        "Detection - Forbidden sign string found"));
            }
        }
        return Result.succeed();
    }
}
