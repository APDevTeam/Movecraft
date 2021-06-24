package net.countercraft.movecraft.processing.tasks.detection.validators;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.CraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.processing.functions.Result;
import net.countercraft.movecraft.processing.functions.DetectionPredicate;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Deque;
import java.util.Map;

public class SizeValidator implements DetectionPredicate<Map<Material, Deque<MovecraftLocation>>> {
    @Override
    public Result validate(@NotNull Map<Material, Deque<MovecraftLocation>> materialDequeMap, @NotNull CraftType type, @NotNull MovecraftWorld world, @Nullable CommandSender player) {
        int size = materialDequeMap.values().parallelStream().map(Deque::size).reduce(Integer::sum).orElse(0);
        if(size > type.getMaxSize()){
            return Result.failWithMessage(String.format(I18nSupport.getInternationalisedString("Detection - Craft too large"), type.getMaxSize()));
        }
        if(size < type.getMinSize()){
            return Result.failWithMessage(String.format(I18nSupport.getInternationalisedString("Detection - Craft too small"), type.getMinSize()));
        }
        return Result.succeed();
    }
}
