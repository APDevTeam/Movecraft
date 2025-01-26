package net.countercraft.movecraft;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.util.MathUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

public class TrackedLocation {

    private int dx;
    private int dy;
    private int dz;

    private WeakReference<Craft> craft;

    /**
     * Creates a new TrackedLocation instance which tracks a location about a craft's midpoint.
     * @param craft The craft this trackedlocation belongs to
     * @param location The absolute position to track. This location will be stored as a relative
     *                 location to the craft's central hitbox.
     */
    public TrackedLocation(@NotNull Craft craft, @NotNull MovecraftLocation location) {
        this.craft = new WeakReference<>(craft);
        reinit(location);
    }

    protected void reinit(@NotNull MovecraftLocation location) {
        reinit(location.getX(), location.getY(), location.getZ());
    }

    protected void reinit(final int x, final int y, final int z) {
        Craft craft = this.getCraft();
        Craft.CraftOrigin origin = craft.getCraftOrigin();
        this.dx = x - origin.getX();
        this.dy = y - origin.getY();
        this.dz = z - origin.getZ();
    }

    /**
     * Rotates the stored location.
     * @param rotation A clockwise or counter-clockwise direction to rotate.
     */
    public void rotate(MovecraftRotation rotation, MovecraftLocation origin) {
        MovecraftLocation absolute = this.getAbsoluteLocation();
        MovecraftLocation vector = MathUtils.rotateVec(rotation, absolute.subtract(origin));

        MovecraftLocation newAbsolute = origin.add(vector);
        reinit(newAbsolute);
    }

    /**
     * Gets the stored absolute location.
     * @return Returns the absolute location instead of a vector.
     */
    public MovecraftLocation getAbsoluteLocation() {
        Craft craft = this.getCraft();
        Craft.CraftOrigin origin = craft.getCraftOrigin();

        int x = origin.getX() + this.dx;
        int y = origin.getX() + this.dy;
        int z = origin.getX() + this.dz;

        return new MovecraftLocation(x, y, z);
    }

    /**
     * NEVER USE THIS UNLESS ABSOLUTELY NECESSARY
     * @param craft
     * @param location
     */
    public void reset(@NotNull Craft craft, @NotNull MovecraftLocation location) {
        this.craft = new WeakReference<>(craft);
        reinit(location);
    }

    public Craft getCraft() {
        if (this.craft.get() == null) {
            throw new RuntimeException("Craft of tracked location is null! This indicates that the craft object was destroyed but somehow the tracked location is still around!");
        }
        return this.craft.get();
    }

}
