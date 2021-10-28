package net.countercraft.movecraft.processing.tasks.translation.validators;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.processing.functions.Result;
import net.countercraft.movecraft.processing.functions.TetradicPredicate;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class MinHeightValidator implements TetradicPredicate<MovecraftLocation, MovecraftWorld, HitBox, CraftType> {
    @Override
    @Contract(pure = true)
    public @NotNull Result validate(@NotNull MovecraftLocation translation, @NotNull MovecraftWorld world, @NotNull HitBox hitBox, @NotNull CraftType type) {
        boolean sinking = Optional.<Boolean>empty().get(); // TODO: sinking
        if (hitBox.getMinX() < type.getMinHeightLimit(world) && translation.getY() < 0 && !sinking && !type.getBoolProperty("useGravity")) {
            return Result.failWithMessage(I18nSupport.getInternationalisedString("Translation - Failed Craft hit minimum height limit"));
        }
        return Result.succeed();
    }
}
