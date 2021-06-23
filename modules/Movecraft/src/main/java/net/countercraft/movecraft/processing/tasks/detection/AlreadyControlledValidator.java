package net.countercraft.movecraft.processing.tasks.detection;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.CraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.processing.functions.Result;
import net.countercraft.movecraft.processing.functions.TaskPredicate;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AlreadyControlledValidator implements TaskPredicate<MovecraftLocation> {
    @Override
    public Result validate(@NotNull MovecraftLocation location, @NotNull CraftType type, @NotNull MovecraftWorld world, @Nullable CommandSender player) {
        return CraftManager.getInstance().getCraftList().stream().filter((c)->c.getMovecraftWorld().equals(world)).map(Craft::getHitBox).anyMatch((h) -> h.contains(location)) ? Result.failWithMessage(I18nSupport.getInternationalisedString("Detection - Failed Craft is already being controlled")) : Result.succeed();
    }
}
