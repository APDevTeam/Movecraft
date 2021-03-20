package net.countercraft.movecraft.processing.tasks.detection;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.CraftType;
import net.countercraft.movecraft.processing.MovecraftWorld;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PilotSignValidator implements DetectionValidator<MovecraftLocation>{
    @Override
    public Modifier validate(@NotNull MovecraftLocation location, @NotNull CraftType type, @NotNull MovecraftWorld world, @Nullable Player player) {
        BlockState state = world.getState(location);
        if (!(state instanceof Sign)) {
            return Modifier.NONE;
        }
        Sign s = (Sign) state;
        if (!s.getLine(0).equalsIgnoreCase("Pilot:") || player == null) {
            return Modifier.NONE;
        }
        String playerName = player.getName();
        boolean foundPilot = false;
        for(int line = 1; line<4; line++){
            if(s.getLine(line).equalsIgnoreCase(playerName)){
                foundPilot = true;
                break;
            }
        }
        return foundPilot || (player.hasPermission("movecraft.bypasslock")) ? Modifier.NONE : Modifier.FAIL;
    }
}
