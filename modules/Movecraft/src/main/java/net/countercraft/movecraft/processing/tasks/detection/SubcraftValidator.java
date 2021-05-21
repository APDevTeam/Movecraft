package net.countercraft.movecraft.processing.tasks.detection;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.CraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.processing.IMovecraftWorld;
import net.countercraft.movecraft.processing.TaskPredicate;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SubcraftValidator implements TaskPredicate<MovecraftLocation> {
    @Override
    public Result validate(@NotNull MovecraftLocation movecraftLocation, @NotNull CraftType type, @NotNull IMovecraftWorld world, @Nullable CommandSender player) {

        if (!type.getMustBeSubcraft()) {
            return Result.succeed();
        }
        Craft foundCraft = null;
        long closestDistSquared = Long.MAX_VALUE;
        for (Craft otherCraft : CraftManager.getInstance().getCraftList()) {
            if (!otherCraft.getMovecraftWorld().equals(world)) {
                continue;
            }
            //this code is copied from CraftManager.FastNearestCraftToLoc
            if(otherCraft.getHitBox().isEmpty())
                continue;
            int midX = (otherCraft.getHitBox().getMaxX() + otherCraft.getHitBox().getMinX()) >> 1;
            int midZ = (otherCraft.getHitBox().getMaxZ() + otherCraft.getHitBox().getMinZ()) >> 1;
            long distSquared = (long) (Math.pow(midX -  movecraftLocation.getX(), 2) + Math.pow(midZ - movecraftLocation.getZ(), 2));
            if (distSquared < closestDistSquared) {
                closestDistSquared = distSquared;
                foundCraft = otherCraft;
            }
        }
        if (foundCraft == null) {
            return Result.failWithMessage("Detection - Must Be Subcraft");
        }

        return foundCraft.getHitBox().contains(movecraftLocation) ? Result.succeed() : Result.failWithMessage(I18nSupport.getInternationalisedString("Detection - Must Be Subcraft"));
    }
}
