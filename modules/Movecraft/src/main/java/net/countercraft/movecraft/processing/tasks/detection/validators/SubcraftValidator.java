package net.countercraft.movecraft.processing.tasks.detection.validators;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.PlayerCraft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.processing.functions.Result;
import net.countercraft.movecraft.processing.functions.DetectionPredicate;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SubcraftValidator implements DetectionPredicate<MovecraftLocation> {
    @Override
    @Contract(pure = true)
    public @NotNull Result validate(@NotNull MovecraftLocation movecraftLocation, @NotNull CraftType type, @NotNull MovecraftWorld world, @Nullable CommandSender player) {

        if (!type.getBoolProperty(CraftType.MUST_BE_SUBCRAFT)) {
            return Result.succeed();
        }

        for (Craft otherCraft : CraftManager.getInstance()) {
            Movecraft.getInstance().getLogger().info(
                    "Checking against craft of type "
                            + otherCraft.getType()
                            + " piloted by "
                            + ((otherCraft instanceof PlayerCraft) ? ((PlayerCraft) otherCraft).getPlayer().getDisplayName() : "null")
                            + " at "
                            + otherCraft.getHitBox().getMidPoint()
            );

            if (!otherCraft.getMovecraftWorld().equals(world)) {
                Movecraft.getInstance().getLogger().info("Wrong world!\t" + otherCraft.getMovecraftWorld().getName());
                continue;
            }
            if (otherCraft.getHitBox().contains(movecraftLocation)) {
                Movecraft.getInstance().getLogger().info("Hitbox is contained!");
                return Result.succeed();
            }
            Movecraft.getInstance().getLogger().info("Hitbox is not contained.");
        }
        return Result.failWithMessage(I18nSupport.getInternationalisedString("Detection - Must Be Subcraft"));
    }
}
