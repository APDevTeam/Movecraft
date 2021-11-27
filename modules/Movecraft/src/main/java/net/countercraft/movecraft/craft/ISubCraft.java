package net.countercraft.movecraft.craft;

import net.countercraft.movecraft.craft.type.CraftType;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public class ISubCraft extends ICraft implements SubCraft {

    public ISubCraft(@NotNull CraftType type, @NotNull World world) {
        super(type, world);
    }

    @Override
    public @NotNull Craft getParent() {
        return null;
    }
}
