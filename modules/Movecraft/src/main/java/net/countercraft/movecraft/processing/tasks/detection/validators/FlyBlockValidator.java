package net.countercraft.movecraft.processing.tasks.detection.validators;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.craft.type.RequiredBlockEntry;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.processing.functions.Result;
import net.countercraft.movecraft.processing.functions.DetectionPredicate;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Deque;
import java.util.Map;

public class FlyBlockValidator implements DetectionPredicate<Map<Material, Deque<MovecraftLocation>>> {
    @Override
    @Contract(pure = true)
    public @NotNull Result validate(@NotNull Map<Material, Deque<MovecraftLocation>> materialDequeMap, @NotNull CraftType type, @NotNull MovecraftWorld world, @Nullable CommandSender player) {
        int total = materialDequeMap.values().parallelStream().mapToInt(Deque::size).sum();
        for (RequiredBlockEntry entry : type.getFlyBlocks()) {
            int count = 0;
            for(Material material : entry.getMaterials()) {
                if(!materialDequeMap.containsKey(material)) {
                    continue;
                }
                count += materialDequeMap.get(material).size();
            }

            var result = entry.detect(count, total);
            if(result.getLeft() == RequiredBlockEntry.DetectionResult.SUCCESS)
                continue;

            String failMessage = "";
            switch (result.getLeft()) {
                case NOT_ENOUGH:
                    failMessage += I18nSupport.getInternationalisedString("Detection - Not enough flyblock");
                    break;
                case TOO_MUCH:
                    failMessage += I18nSupport.getInternationalisedString("Detection - Too much flyblock");
                    break;
                default:
                    break;
            }
            failMessage += ": [" + entry.materialsToString() + "] " + result.getRight();
            return Result.failWithMessage(failMessage);
        }
        return Result.succeed();
    }
}
