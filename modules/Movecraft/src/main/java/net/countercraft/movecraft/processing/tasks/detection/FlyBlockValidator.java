package net.countercraft.movecraft.processing.tasks.detection;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.CraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.processing.MovecraftWorld;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Deque;
import java.util.List;
import java.util.Map;

public class FlyBlockValidator implements DetectionValidator<Map<Material, Deque<MovecraftLocation>>>{
    @Override
    public Modifier validate(@NotNull Map<Material, Deque<MovecraftLocation>> materialDequeMap, @NotNull CraftType type, @NotNull MovecraftWorld world, @Nullable Player player) {
        int total = materialDequeMap.values().stream().mapToInt(Deque::size).sum();
        var flyBlocks = type.getFlyBlocks();
        for (List<Material> i : flyBlocks.keySet()) {
            int numberOfBlocks = 0;
            for(Material material : i){
                if(!materialDequeMap.containsKey(material)){
                    continue;
                }
                numberOfBlocks += materialDequeMap.get(material).size();
            }

            float blockPercentage = (((float) numberOfBlocks / total) * 100);
            Double minPercentage = flyBlocks.get(i).get(0);
            Double maxPercentage = flyBlocks.get(i).get(1);
            if (minPercentage < 10000.0) {
                if (blockPercentage < minPercentage) {
                    fail(String.format(
                            I18nSupport.getInternationalisedString("Detection - Not enough flyblock") + ": %s %.2f%% < %.2f%%",
                            i.get(0).name().toLowerCase().replace("_", " "), blockPercentage,
                            minPercentage));
                    return Modifier.FAIL;
                }
            } else {
                if (numberOfBlocks < flyBlocks.get(i).get(0) - 10000.0) {
                    fail(String.format(
                            I18nSupport.getInternationalisedString("Detection - Not enough flyblock") + ": %s %d < %d",
                            i.get(0).name().toLowerCase().replace("_", " "), numberOfBlocks,
                            flyBlocks.get(i).get(0).intValue() - 10000));
                    return Modifier.FAIL;
                }
            }
            if (maxPercentage < 10000.0) {
                if (blockPercentage > maxPercentage) {
                    fail(String.format(
                            I18nSupport.getInternationalisedString("Detection - Too much flyblock") + ": %s %.2f%% > %.2f%%",
                            i.get(0).name().toLowerCase().replace("_", " "), blockPercentage,
                            maxPercentage));
                    return Modifier.FAIL;
                }
            } else {
                if (numberOfBlocks > flyBlocks.get(i).get(1) - 10000.0) {
                    fail(String.format(I18nSupport.getInternationalisedString("Detection - Too much flyblock") + ": %s %d > %d",
                            i.get(0).name().toLowerCase().replace("_", " "), numberOfBlocks,
                            flyBlocks.get(i).get(1).intValue() - 10000));
                    return Modifier.FAIL;
                }
            }
        }

        return Modifier.NONE;
    }

    private void fail(String format) {
        //noop
    }
}
