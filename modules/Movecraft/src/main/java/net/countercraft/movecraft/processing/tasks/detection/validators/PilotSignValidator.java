package net.countercraft.movecraft.processing.tasks.detection.validators;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.processing.functions.DetectionPredicate;
import net.countercraft.movecraft.processing.functions.Result;
import org.bukkit.Tag;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PilotSignValidator implements DetectionPredicate<MovecraftLocation> {
    @Override
    @Contract(pure = true)
    public @NotNull Result validate(@NotNull MovecraftLocation movecraftLocation, @NotNull CraftType type, @NotNull MovecraftWorld world, @Nullable Player player) {
        if(!Tag.SIGNS.isTagged(world.getMaterial(movecraftLocation))){
            return Result.succeed();
        }
        BlockState state = world.getState(movecraftLocation);
        if (!(state instanceof Sign)) {
            return Result.succeed();
        }
        Sign s = (Sign) state;
        if (!s.getLine(0).equalsIgnoreCase("Pilot:") || player == null) {
            return Result.succeed();
        }
        String playerName = player.getName();
        boolean foundPilot = false;
        for(int line = 1; line<4; line++){
            if(s.getLine(line).equalsIgnoreCase(playerName)){
                foundPilot = true;
                break;
            }
        }
        return foundPilot || (player.hasPermission("movecraft.bypasslock")) ? Result.succeed() : Result.failWithMessage(I18nSupport.getInternationalisedString("Detection - Not Registered Pilot"));
    }
}
