package net.countercraft.movecraft.craft;

import net.countercraft.movecraft.craft.type.CraftType;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PlayerCraftImpl extends BaseCraft implements PlayerCraft {
    private final UUID id = UUID.randomUUID();
    private final int hashCode = id.hashCode();
    private final Player pilot;
    private boolean pilotLocked;
    private double pilotLockedX;
    private double pilotLockedY;
    private double pilotLockedZ;

    public PlayerCraftImpl(@NotNull CraftType type, @NotNull World world, @NotNull Player pilot) {
        super(type, world);
        this.pilot = pilot;
        pilotLocked = false;
        pilotLockedX = 0.0;
        pilotLockedY = 0.0;
        pilotLockedZ = 0.0;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PlayerCraftImpl))
            return false;

        return id.equals(((PlayerCraftImpl) obj).id);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @NotNull
    @Override
    public Player getPilot() {
        return pilot;
    }

    public boolean getPilotLocked() {
        return pilotLocked;
    }

    public void setPilotLocked(boolean pilotLocked) {
        this.pilotLocked = pilotLocked;
    }

    public double getPilotLockedX() {
        return pilotLockedX;
    }

    public void setPilotLockedX(double pilotLockedX) {
        this.pilotLockedX = pilotLockedX;
    }

    public double getPilotLockedY() {
        return pilotLockedY;
    }

    public void setPilotLockedY(double pilotLockedY) {
        this.pilotLockedY = pilotLockedY;
    }

    public double getPilotLockedZ() {
        return pilotLockedZ;
    }

    public void setPilotLockedZ(double pilotLockedZ) {
        this.pilotLockedZ = pilotLockedZ;
    }
}
