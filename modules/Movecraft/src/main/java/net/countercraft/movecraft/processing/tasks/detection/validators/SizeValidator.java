package net.countercraft.movecraft.processing.tasks.detection.validators;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.processing.functions.DetectionPredicate;
import net.countercraft.movecraft.processing.functions.Result;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Deque;
import java.util.Map;

public class SizeValidator implements DetectionPredicate<Map<Material, Deque<MovecraftLocation>>> {
    @Override
    @Contract(pure = true)
    public @NotNull Result validate(@NotNull Map<Material, Deque<MovecraftLocation>> materialDequeMap, @NotNull CraftType type, @NotNull MovecraftWorld world, @Nullable Player player) {
        int size = materialDequeMap.values().parallelStream().mapToInt(Deque::size).sum();
        if(size > type.getIntProperty(CraftType.MAX_SIZE)) {
            return Result.failWithMessage(String.format(I18nSupport.getInternationalisedString("Detection - Craft too large"), type.getIntProperty(CraftType.MAX_SIZE)));
        }
        if(size < type.getIntProperty(CraftType.MIN_SIZE)) {
            return Result.failWithMessage(String.format(I18nSupport.getInternationalisedString("Detection - Craft too small"), type.getIntProperty(CraftType.MIN_SIZE)));
        }
        return Result.succeed();
    }

}
