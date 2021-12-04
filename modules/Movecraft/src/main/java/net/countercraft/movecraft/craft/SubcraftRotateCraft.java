package net.countercraft.movecraft.craft;

import net.countercraft.movecraft.craft.type.CraftType;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SubcraftRotateCraft extends BaseCraft implements PilotedCraft {
    private final Player pilot;

    public SubcraftRotateCraft(@NotNull CraftType type, @NotNull World world, @NotNull Player pilot) {
        super(type, world);
        this.pilot = pilot;
    }

    @Override
    public @NotNull Player getPilot() {
        return pilot;
    }
}
