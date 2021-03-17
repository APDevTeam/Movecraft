package net.countercraft.movecraft.processing.tasks.detection;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.CraftType;
import net.countercraft.movecraft.processing.MovecraftWorld;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NameSignValidator implements DetectionValidator{
    @Override
    public Modifier validate(@NotNull MovecraftLocation location, @NotNull CraftType type, @NotNull MovecraftWorld world, @Nullable Player player) {
        BlockState state = world.getState(location);
        if (!(state instanceof Sign)) {
            return Modifier.NONE;
        }
        Sign s = (Sign) state;
        return s.getLine(0).equalsIgnoreCase("Name:") && !type.getCanBeNamed() ? Modifier.FAIL : Modifier.NONE;
    }
}
