package net.countercraft.movecraft.craft;

import net.countercraft.movecraft.craft.type.CraftType;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PilotedCraft extends BaseCraft implements PlayerCraft{
    private final UUID id = UUID.randomUUID();
    private final Player pilot;
    private boolean pilotLocked;
    private double pilotLockedX;
    private double pilotLockedY;
    private double pilotLockedZ;
    public PilotedCraft(@NotNull CraftType type, @NotNull World world, @NotNull Player player) {
        super(type, world);
        this.pilot = player;
        this.pilotLocked = false;
        this.pilotLockedX = 0.0;
        this.pilotLockedY = 0.0;
        this.pilotLockedZ = 0.0;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof PilotedCraft)){
            return false;
        }
        return this.id.equals(((PilotedCraft)obj).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @NotNull
    @Override
    public Player getPlayer() {
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
