package net.countercraft.movecraft.processing.tasks.translation.validators;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.type.PropertyKeys;
import net.countercraft.movecraft.craft.type.TypeSafeCraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.processing.functions.Result;
import net.countercraft.movecraft.processing.functions.TetradicPredicate;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class MaxHeightValidator implements TetradicPredicate<MovecraftLocation, MovecraftWorld, HitBox, TypeSafeCraftType> {
    @Override
    @Contract(pure = true)
    public @NotNull Result validate(@NotNull MovecraftLocation translation, @NotNull MovecraftWorld world, @NotNull HitBox hitBox, @NotNull TypeSafeCraftType type){
        int maxHeightLimit = type.get(PropertyKeys.MAX_HEIGHT_LIMIT, world);
        if (translation.getY() > 0 && hitBox.getMaxY() > maxHeightLimit && type.get(PropertyKeys.COLLISION_EXPLOSION) <= 0F) {
            return Result.failWithMessage(I18nSupport.getInternationalisedString("Translation - Failed Craft hit height limit"));
        }
        return Result.succeed();
    }
}
