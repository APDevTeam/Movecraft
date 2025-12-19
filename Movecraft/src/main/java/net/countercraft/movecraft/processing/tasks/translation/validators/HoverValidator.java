package net.countercraft.movecraft.processing.tasks.translation.validators;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.type.PropertyKeys;
import net.countercraft.movecraft.craft.type.TypeSafeCraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.processing.functions.Result;
import net.countercraft.movecraft.processing.functions.TetradicPredicate;
import net.countercraft.movecraft.util.NamespacedIDUtil;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class HoverValidator implements TetradicPredicate<MovecraftLocation, MovecraftWorld, HitBox, TypeSafeCraftType> {
    @Override
    @Contract(pure = true)
    public @NotNull Result validate(@NotNull MovecraftLocation translation, @NotNull MovecraftWorld world, @NotNull HitBox hitBox, @NotNull TypeSafeCraftType type) {
        if (type.get(PropertyKeys.FORBIDDEN_HOVER_OVER_BLOCKS).size() > 0){
            MovecraftLocation test = new MovecraftLocation(hitBox.getMidPoint().getX(), hitBox.getMinY(), hitBox.getMidPoint().getZ());
            test = test.translate(0, -1, 0);
            while (world.getMaterial(test).isAir()) {
                test = test.translate(0, -1, 0);
            }
            NamespacedKey testType = NamespacedIDUtil.getBlockID(world.getData(test));
            if (type.get(PropertyKeys.FORBIDDEN_HOVER_OVER_BLOCKS).contains(testType)){
                return Result.failWithMessage(String.format(I18nSupport.getInternationalisedString("Translation - Failed Craft over block"), testType.toString().replace("_", " ")));
            }
        }
        return Result.succeed();
    }
}
