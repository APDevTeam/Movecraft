package net.countercraft.movecraft.processing.tasks.detection.validators;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.processing.functions.DetectionPredicate;
import net.countercraft.movecraft.processing.functions.Result;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Deque;
import java.util.Map;

public class LiquidBlockValidator implements DetectionPredicate<Map<Material, Deque<MovecraftLocation>>> {
    @Override
    public @NotNull Result validate(@NotNull Map<Material, Deque<MovecraftLocation>> materialDequeMap, @NotNull CraftType type, @NotNull MovecraftWorld world, @Nullable Player player) {
        String allowedAmountString = type.getStringProperty(CraftType.LIQUIDS_MAX_AMOUNT);
        boolean blockNumber = !allowedAmountString.startsWith("N"); //if false use block percentage
        final String errorMessage = "Too many waterlogged blocks on craft";

        int maxAmount;
        try {
            if (blockNumber) {
                maxAmount = Integer.parseInt(allowedAmountString.substring(1));
            } else {
                maxAmount = Integer.parseInt(allowedAmountString);
            }
        } catch (NumberFormatException e) {
            return Result.failWithMessage("waterloggedMaxAmount wasn't configurated properly");
        }

        final int waterLoggedBlocks = getWaterLoggedAmount(materialDequeMap, world);
        if (waterLoggedBlocks == 0)
            return Result.succeed();

        if (maxAmount == 0)
            return Result.failWithMessage(errorMessage);

        if(blockNumber) {
            if (waterLoggedBlocks <= maxAmount) {
                return Result.succeed();
            } else {
                return Result.failWithMessage(errorMessage);
            }
        } else {
            int allBlocks = getTotalBlocks(materialDequeMap);
            double percentage = ((double) waterLoggedBlocks / allBlocks) * 100;

            if (percentage <= maxAmount) {
                return Result.succeed();
            } else {
                return Result.failWithMessage(errorMessage);
            }
        }
    }

    public int getWaterLoggedAmount(Map<Material, Deque<MovecraftLocation>> materialDequeMap, MovecraftWorld world) {
        int amount = 0;
        for (var locationList : materialDequeMap.values()) {
            for (var location : locationList) {
                BlockData blockData = world.getData(location);
                if (blockData instanceof Waterlogged waterlogged) {
                    if (waterlogged.isWaterlogged()) amount++;
                }
            }
        }

        return amount;
    }

    public int getTotalBlocks(Map<Material, Deque<MovecraftLocation>> materialDequeMap) {
        int amount = 0;
        for (var locationList : materialDequeMap.values()) {
            amount += locationList.size();
        }

        return amount;
    }
}
