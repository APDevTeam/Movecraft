package net.countercraft.movecraft.processing.tasks.detection.validators;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.type.PropertyKeys;
import net.countercraft.movecraft.craft.type.TypeSafeCraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.processing.functions.DetectionPredicate;
import net.countercraft.movecraft.processing.functions.Result;
import org.bukkit.Tag;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class ForbiddenSignStringValidator implements DetectionPredicate<MovecraftLocation> {
    @Override
    @Contract(pure = true)
    public @NotNull Result validate(@NotNull MovecraftLocation movecraftLocation, @NotNull TypeSafeCraftType type, @NotNull MovecraftWorld world, @Nullable Player player) {
        if(!Tag.SIGNS.isTagged(world.getMaterial(movecraftLocation))){
            return Result.succeed();
        }
        BlockState state = world.getState(movecraftLocation);
        if (!(state instanceof Sign)) {
            return Result.succeed();
        }
        Sign sign = (Sign) state;

        var object = type.get(PropertyKeys.FORBIDDEN_SIGN_STRINGS);

        for(var line : sign.getLines()){
            if(object.contains(line.toLowerCase())){
                return Result.failWithMessage(I18nSupport.getInternationalisedString(
                        "Detection - Forbidden sign string found"));
            }
        }
        return Result.succeed();
    }

}
