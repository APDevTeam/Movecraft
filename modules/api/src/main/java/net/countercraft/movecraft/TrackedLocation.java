package net.countercraft.movecraft;

import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public class TrackedLocation {
    private int posX;
    private int posY;
    private int posZ;
    private MovecraftLocation origin;

    /**
     * Creates a new TrackedLocation instance which tracks a location about an origin.
     * @param origin The origin point at which the tracked location rotates about.
     * @param location The absolute location to track.
     */
    public TrackedLocation(@NotNull MovecraftLocation origin, @NotNull MovecraftLocation location) {
        this.origin = origin;
        posX = location.getX() - origin.getX();
        posY = location.getY() - origin.getY();
        posZ = location.getZ() - origin.getZ();
    }

    /**
     * Moves the origin point of the tracked location
     */
    public void translate(int dx, int dy, int dz) {
        origin = new MovecraftLocation(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
    }

    /**
     * Rotates the stored location about the origin point location.
     * @param rotation A clockwise or counter-clockwise direction to rotate.
     */
    public void rotate(MovecraftRotation rotation) {
        int newX;
        int newZ;
        if (rotation == MovecraftRotation.CLOCKWISE) {
            newX = posZ + origin.getX();
            newZ = -posX + origin.getZ();
        } else if (rotation == MovecraftRotation.ANTICLOCKWISE) {
            newX = -posZ + origin.getX();
            newZ = posX + origin.getZ();
        } else return;

        posX = newX;
        posZ = newZ;
    }

    /**
     * Gets the stored location.
     * @return Returns the location.
     */
    public MovecraftLocation getLocation() {
        return new MovecraftLocation(posX + origin.getX(), posY + origin.getY(), posZ + origin.getZ());
    }
}
