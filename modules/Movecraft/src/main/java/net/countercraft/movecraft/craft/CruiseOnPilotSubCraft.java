package net.countercraft.movecraft.craft;

import net.countercraft.movecraft.craft.type.CraftType;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CruiseOnPilotSubCraft extends CruiseOnPilotCraft implements SubCraft {
    @NotNull
    private Craft parent;

    public CruiseOnPilotSubCraft(@NotNull CraftType type, @NotNull World world, @NotNull Player pilot, @NotNull Craft parent) {
        super(type, world, pilot);
        this.parent = parent;
    }

    @Override
    public @NotNull Craft getParent() {
        return parent;
    }

    @Override
    public void setParent(@NotNull Craft parent) {
        this.parent = parent;
    }
}
