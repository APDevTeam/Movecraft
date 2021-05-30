package net.countercraft.movecraft.processing.tasks.detection;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.CraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.processing.TaskPredicate;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SubcraftValidator implements TaskPredicate<MovecraftLocation> {
    @Override
    public Result validate(@NotNull MovecraftLocation movecraftLocation, @NotNull CraftType type, @NotNull MovecraftWorld world, @Nullable CommandSender player) {

        if (!type.getMustBeSubcraft()) {
            return Result.succeed();
        }

        for (Craft otherCraft : CraftManager.getInstance().getCraftList()) {
            if (!otherCraft.getMovecraftWorld().equals(world)) {
                continue;
            }
            if (otherCraft.getHitBox().contains(movecraftLocation)) {
                return Result.succeed();
            }
        }
        return Result.failWithMessage(I18nSupport.getInternationalisedString("Detection - Must Be Subcraft"));
    }
}
