package net.countercraft.movecraft.processing.tasks.detection.validators;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.craft.type.RequiredBlockEntry;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.processing.functions.DetectionPredicate;
import net.countercraft.movecraft.processing.functions.Result;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Deque;
import java.util.Map;

public abstract class AbstractBlockConstraintValidator implements DetectionPredicate<Map<Material, Deque<MovecraftLocation>>> {

    protected abstract Collection<RequiredBlockEntry> getRelevantConstraintSet(final CraftType type);
    protected abstract String getFailMessage(RequiredBlockEntry.DetectionResult result, @NotNull String errorMessage, RequiredBlockEntry failedCondition);

    @Override
    @Contract(pure = true)
    public @NotNull Result validate(@NotNull Map<Material, Deque<MovecraftLocation>> materialDequeMap,
                                    @NotNull CraftType type, @NotNull MovecraftWorld world, @Nullable Player player) {
        int total = materialDequeMap.values().parallelStream().mapToInt(Deque::size).sum();
        for (RequiredBlockEntry entry : getRelevantConstraintSet(type)) {
            int count = 0;
            for (Material material : entry.getMaterials()) {
                if (!materialDequeMap.containsKey(material))
                    continue;

                count += materialDequeMap.get(material).size();
            }

            var result = entry.detect(count, total);
            if (result.getLeft() == RequiredBlockEntry.DetectionResult.SUCCESS)
                continue;

            String failMessage = getFailMessage(result.getLeft(), result.getRight(), entry);
            return Result.failWithMessage(failMessage);
        }
        return Result.succeed();
    }
}
