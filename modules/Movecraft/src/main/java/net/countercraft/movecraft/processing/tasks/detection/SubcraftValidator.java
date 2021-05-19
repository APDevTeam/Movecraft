package net.countercraft.movecraft.processing.tasks.detection;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.CraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.processing.TaskPredicate;
import net.countercraft.movecraft.processing.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class SubcraftValidator implements TaskPredicate<MovecraftLocation> {
    @Override
    public Result validate(@NotNull MovecraftLocation movecraftLocation, @NotNull CraftType type, @NotNull MovecraftWorld world, @Nullable CommandSender player) {

        if (!type.getMustBeSubcraft()) {
            return Result.succeed();
        }

        //This has to be executed on the main thread because otherwise it could cause deadlock
        return WorldManager.INSTANCE.executeMain(new Supplier<Result>() {
            @Override
            public Result get() {
                Craft otherCraft = CraftManager.getInstance().fastNearestCraftToLoc(movecraftLocation.toBukkit(Bukkit.getWorld(world.getWorldUUID())));
                if (otherCraft == null) {
                    return Result.failWithMessage(I18nSupport.getInternationalisedString("Detection - Must Be Subcraft"));
                }
                return (otherCraft.getHitBox().contains(movecraftLocation) ? Result.succeed() : Result.failWithMessage(I18nSupport.getInternationalisedString("Detection - Must Be Subcraft")));
            }
        });
    }
}
