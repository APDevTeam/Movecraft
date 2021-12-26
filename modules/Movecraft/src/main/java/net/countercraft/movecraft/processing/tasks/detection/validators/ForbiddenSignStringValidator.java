package net.countercraft.movecraft.processing.tasks.detection.validators;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.type.CraftType;
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
    public @NotNull Result validate(@NotNull MovecraftLocation movecraftLocation, @NotNull CraftType type, @NotNull MovecraftWorld world, @Nullable Player player) {
        if(!Tag.SIGNS.isTagged(world.getMaterial(movecraftLocation))){
            return Result.succeed();
        }
        BlockState state = world.getState(movecraftLocation);
        if (!(state instanceof Sign)) {
            return Result.succeed();
        }
        Sign sign = (Sign) state;

        var object = type.getObjectProperty(CraftType.FORBIDDEN_SIGN_STRINGS);
        if(!(object instanceof Collection<?>))
            throw new IllegalStateException("FORBIDDEN_SIGN_STRINGS must be of type Collection");
        var collection = ((Collection<?>) object);
        collection.forEach(i -> {
            if(!(i instanceof String))
                throw new IllegalStateException("Values in FORBIDDEN_SIGN_STRINGS must be of type String");
        });

        for(var line : sign.getLines()){
            if(collection.contains(line.toLowerCase())){
                return Result.failWithMessage(I18nSupport.getInternationalisedString(
                        "Detection - Forbidden sign string found"));
            }
        }
        return Result.succeed();
    }

}
