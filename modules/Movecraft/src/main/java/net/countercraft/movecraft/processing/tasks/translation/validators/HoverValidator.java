package net.countercraft.movecraft.processing.tasks.translation.validators;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.CraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.processing.functions.Result;
import net.countercraft.movecraft.processing.functions.TetradicPredicate;
import net.countercraft.movecraft.processing.functions.TriadicPredicate;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

public class HoverValidator implements TetradicPredicate<MovecraftLocation, MovecraftWorld, HitBox, CraftType> {
    @Override
    public @NotNull Result validate(@NotNull MovecraftLocation translation, @NotNull MovecraftWorld world, @NotNull HitBox hitBox, @NotNull CraftType type) {
        if (type.getForbiddenHoverOverBlocks().size() > 0){
            MovecraftLocation test = new MovecraftLocation(hitBox.getMidPoint().getX(), hitBox.getMinY(), hitBox.getMidPoint().getZ());
            test = test.translate(0, -1, 0);
            while (world.getMaterial(test) == Material.AIR){
                test = test.translate(0, -1, 0);
            }
            Material testType = world.getMaterial(test);
            if (type.getForbiddenHoverOverBlocks().contains(testType)){
                return Result.failWithMessage(String.format(I18nSupport.getInternationalisedString("Translation - Failed Craft over block"), testType.name().toLowerCase().replace("_", " ")));
            }
        }
        return Result.succeed();
    }
}
