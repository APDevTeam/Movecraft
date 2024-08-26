package net.countercraft.movecraft.craft.controller;

public interface PlayerController extends PilotController {
    boolean getPilotLocked();

    void setPilotLocked(boolean pilotLocked);

    double getPilotLockedX();

    void setPilotLockedX(double pilotLockedX);

    double getPilotLockedY();

    void setPilotLockedY(double pilotLockedY);

    double getPilotLockedZ();

    void setPilotLockedZ(double pilotLockedZ);
}
