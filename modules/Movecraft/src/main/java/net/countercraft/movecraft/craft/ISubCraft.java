package net.countercraft.movecraft.craft;

import net.countercraft.movecraft.craft.type.CraftType;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public class ISubCraft extends ICraft implements SubCraft {
    private final Craft parent;

    public ISubCraft(@NotNull CraftType type, @NotNull World world, @NotNull Craft parent) {
        super(type, world);
        this.parent = parent;
    }

    @Override
    public @NotNull Craft getParent() {
        return parent;
    }
}
