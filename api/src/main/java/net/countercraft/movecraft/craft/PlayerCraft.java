package net.countercraft.movecraft.craft;

public interface PlayerCraft extends PilotedCraft {

    boolean getPilotLocked();

    void setPilotLocked(boolean pilotLocked);

    double getPilotLockedX();

    void setPilotLockedX(double pilotLockedX);

    double getPilotLockedY();

    void setPilotLockedY(double pilotLockedY);

    double getPilotLockedZ();

    void setPilotLockedZ(double pilotLockedZ);
}
