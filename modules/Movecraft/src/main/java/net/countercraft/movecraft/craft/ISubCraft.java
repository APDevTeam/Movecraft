package net.countercraft.movecraft.craft;

import net.countercraft.movecraft.craft.type.CraftType;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public class ISubCraft extends BaseCraft implements SubCraft {
    private Craft parent;

    public ISubCraft(@NotNull CraftType type, @NotNull World world) {
        super(type, world);
    }

    @Override
    @NotNull
    public Craft getParent() {
        return parent;
    }

    @Override
    public void setParent(@NotNull Craft parent) {
        this.parent = parent;
    }
}
