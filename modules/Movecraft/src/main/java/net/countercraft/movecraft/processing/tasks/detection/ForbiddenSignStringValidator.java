package net.countercraft.movecraft.processing.tasks.detection;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.CraftType;
import net.countercraft.movecraft.processing.MovecraftWorld;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ForbiddenSignStringValidator implements DetectionValidator {
    @Override
    public Modifier validate(@NotNull MovecraftLocation location, @NotNull CraftType type, @NotNull MovecraftWorld world, @Nullable Player player) {
        BlockState state = world.getState(location);
        if (!(state instanceof Sign)) {
            return Modifier.NONE;
        }
        Sign sign = (Sign) state;
        for(var line : sign.getLines()){
            if(type.getForbiddenSignStrings().contains(line.toLowerCase())){
                return Modifier.FAIL;
            }
        }
        return Modifier.NONE;
    }
}
