package net.countercraft.movecraft.processing.tasks.translation.validators;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.processing.functions.Result;
import net.countercraft.movecraft.processing.functions.TetradicPredicate;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import org.bukkit.Material;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class HoverValidator implements TetradicPredicate<MovecraftLocation, MovecraftWorld, HitBox, CraftType> {
    @Override
    @Contract(pure = true)
    public @NotNull Result validate(@NotNull MovecraftLocation translation, @NotNull MovecraftWorld world, @NotNull HitBox hitBox, @NotNull CraftType type) {

            //If its inverted set to allow hover over blocks 
            if (craft.getType().getBoolProperty(CraftType.INVERT_HOVER_OVER_BLOCKS)) {
                if (type.getMaterialSetProperty(CraftType.ALLOW_HOVER_OVER_BLOCKS).size() > 0){
                MovecraftLocation test = new MovecraftLocation(hitBox.getMidPoint().getX(), hitBox.getMinY(), hitBox.getMidPoint().getZ());
                test = test.translate(0, -1, 0);
                while (world.getMaterial(test).isAir()) {
                    test = test.translate(0, -1, 0);
                }
                Material testType = world.getMaterial(test);
                if (!type.getMaterialSetProperty(CraftType.ALLOW_HOVER_OVER_BLOCKS).contains(testType)){
                    return Result.failWithMessage(String.format(I18nSupport.getInternationalisedString("Translation - Failed Craft over block"), testType.name().toLowerCase().replace("_", " ")));
                    }
                }               
            }   
            //Else set to forbidden hover over blocks 
            else { 
                if (type.getMaterialSetProperty(CraftType.FORBIDDEN_HOVER_OVER_BLOCKS).size() > 0){
                    MovecraftLocation test = new MovecraftLocation(hitBox.getMidPoint().getX(), hitBox.getMinY(), hitBox.getMidPoint().getZ());
                    test = test.translate(0, -1, 0);
                    while (world.getMaterial(test).isAir()) {
                        test = test.translate(0, -1, 0);
                    }
                    Material testType = world.getMaterial(test);
                    if (type.getMaterialSetProperty(CraftType.FORBIDDEN_HOVER_OVER_BLOCKS).contains(testType)){
                        return Result.failWithMessage(String.format(I18nSupport.getInternationalisedString("Translation - Failed Craft over block"), testType.name().toLowerCase().replace("_", " ")));
                    }
                }
            }
        return Result.succeed();
    }
}
