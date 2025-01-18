package net.countercraft.movecraft;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.util.MathUtils;
import org.jetbrains.annotations.NotNull;

public class TrackedLocation {

    private int x;
    private int y;
    private int z;

    /**
     * Creates a new TrackedLocation instance which tracks a location about a craft's midpoint.
     * @param location The absolute position to track. This location will be stored as a relative
     *                 location to the craft's central hitbox.
     */
    public TrackedLocation(@NotNull MovecraftLocation location) {
        reinit(location);
    }

    @Deprecated(forRemoval = true)
    public TrackedLocation(@NotNull Craft craft, @NotNull MovecraftLocation location) {
        this(location);
    }

    protected void reinit(@NotNull MovecraftLocation location) {
        reinit(location.getX(), location.getY(), location.getZ());
    }

    protected void reinit(final int x, final int y, final int z) {
        this.x = x;
        this.y = y;
        this.z = z;
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

    public void translate(final int dx, final int dy, final int dz) {
        this.x += dx;
        this.y += dy;
        this.z += dz;
    }

    /**
     * Gets the stored absolute location.
     * @return Returns the absolute location instead of a vector.
     */
    public MovecraftLocation getAbsoluteLocation() {
        return new MovecraftLocation(this.x, this. y, this.z);
    }

    /**
     * NEVER USE THIS UNLESS ABSOLUTELY NECESSARY
     * @param craft
     * @param location
     */
    @Deprecated(forRemoval = true)
    public void reset(@NotNull Craft craft, @NotNull MovecraftLocation location) {
        reset(location);
    }

    public void reset(@NotNull MovecraftLocation location) {
        reinit(location);
    }

}
