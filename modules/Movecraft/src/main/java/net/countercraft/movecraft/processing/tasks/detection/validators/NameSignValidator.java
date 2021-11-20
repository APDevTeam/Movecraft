package net.countercraft.movecraft.processing.tasks.detection.validators;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.processing.functions.Result;
import net.countercraft.movecraft.processing.functions.DetectionPredicate;
import org.bukkit.Tag;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NameSignValidator implements DetectionPredicate<MovecraftLocation> {
    @Override
    @Contract(pure = true)
    public @NotNull Result validate(@NotNull MovecraftLocation location, @NotNull CraftType type, @NotNull MovecraftWorld world, @Nullable CommandSender player) {
        if(!Tag.SIGNS.isTagged(world.getMaterial(location))){
            return Result.succeed();
        }
        BlockState state = world.getState(location);
        if (!(state instanceof Sign)) {
            return Result.succeed();
        }
        Sign s = (Sign) state;
        return s.getLine(0).equalsIgnoreCase("Name:") && !type.getBoolProperty(CraftType.CAN_BE_NAMED) ? Result.failWithMessage(I18nSupport.getInternationalisedString("Detection - Craft Type Cannot Be Named")) : Result.succeed();
    }
}
