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
        // TODO: SOMEHOW "absolute" and "origin" can end up to be the same thing?! WTF?!
        MovecraftLocation absolute = this.getAbsoluteLocation();
        System.out.println("Absolute: " + absolute.toString());
        MovecraftLocation vector = MathUtils.rotateVec(rotation, absolute.subtract(origin));
        //MovecraftLocation vector = absolute.subtract(origin);
        //int x = rotation == MovecraftRotation.ANTICLOCKWISE ? vector.getZ() : -vector.getZ();
        //int z = rotation == MovecraftRotation.ANTICLOCKWISE ? -vector.getX() : vector.getX();
        //vector = new MovecraftLocation(x, vector.getY(), z);

        MovecraftLocation newAbsolute = origin.add(vector);
        System.out.println("New absolute: " + newAbsolute.toString());
        // Ugly hack, but necessary
        //MovecraftLocation craftMidPoint = this.craft.getHitBox().getMidPoint();
        //this.offSet = newAbsolute.subtract(craftMidPoint);
        reinit(newAbsolute);

        System.out.println("New absolute after recalculating: " + this.getAbsoluteLocation().toString());
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
    public void reset(@NotNull Craft craft, @NotNull MovecraftLocation location) {
        /*if (craft == this.craft) {
            return;
        }
        if (this.craft != null) {
            if (!(
                    // From parent to subcraft
                    (craft instanceof SubCraft subCraft && subCraft.getParent() == this.craft)
                    // From subcraft back to parent
                    || (this.craft instanceof SubCraft subCraft2 && subCraft2.getParent() == craft)
                )) {
                throw new IllegalStateException("Only ever call this when transferring from or to subcraft!");
            }
        }
        this.craft = craft;
        MovecraftLocation midPoint = craft.getHitBox().getMidPoint();
        System.out.println("Location: " + location.toString());
        System.out.println("Midpoint: " + midPoint.toString());
        offSet = location.subtract(midPoint);*/
        reinit(location);
    }

}
