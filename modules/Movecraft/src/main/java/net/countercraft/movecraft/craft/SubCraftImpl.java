package net.countercraft.movecraft.craft;

import net.countercraft.movecraft.craft.type.CraftType;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public class SubCraftImpl extends BaseCraft implements SubCraft {
    @NotNull
    private Craft parent;

    public SubCraftImpl(@NotNull CraftType type, @NotNull World world, @NotNull Craft parent) {
        super(type, world);
        this.parent = parent;
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
