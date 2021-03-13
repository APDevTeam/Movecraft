package net.countercraft.movecraft.craft;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface PlayerCraft extends Craft{

    @NotNull
    Player getPlayer();

    boolean getPilotLocked();

    void setPilotLocked(boolean pilotLocked);

    double getPilotLockedX();

    void setPilotLockedX(double pilotLockedX);

    double getPilotLockedY();

    void setPilotLockedY(double pilotLockedY);

    double getPilotLockedZ();

    void setPilotLockedZ(double pilotLockedZ);
}
