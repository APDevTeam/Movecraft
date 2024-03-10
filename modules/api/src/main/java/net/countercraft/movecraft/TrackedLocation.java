package net.countercraft.movecraft;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.util.MathUtils;
import org.jetbrains.annotations.NotNull;

public class TrackedLocation {
    private MovecraftLocation offSet;
    private final Craft craft;

    /**
     * Creates a new TrackedLocation instance which tracks a location about a craft's midpoint.
     * @param craft The craft that's that tied to the location.
     * @param location The absolute position to track. This location will be stored as a relative
     *                 location to the craft's central hitbox.
     */
    public TrackedLocation(@NotNull Craft craft, @NotNull MovecraftLocation location) {
        this.craft = craft;
        MovecraftLocation midPoint = craft.getHitBox().getMidPoint();
        offSet = new MovecraftLocation(
                location.getX() - midPoint.getX(),
                location.getY() - midPoint.getY(),
                location.getZ() - midPoint.getZ()
        );
    }

    /**
     * Rotates the stored location about any point.
     * @param rotation A clockwise or counter-clockwise direction to rotate.
     * @param pivot An absolute position at which to rotate the trackedLocation.
     */
    public void rotate(MovecraftRotation rotation, MovecraftLocation pivot) {
        //This is the position vector relative to the tracked location to the pivot.
        MovecraftLocation offSetToPivot = new MovecraftLocation(
                getAbsoluteLocation().getX() - pivot.getX(),
                getAbsoluteLocation().getY() - pivot.getY(),
                getAbsoluteLocation().getZ() - pivot.getZ()
        );
        //This is the position vector relative to the midpoint to the pivot.
        MovecraftLocation midPoint = craft.getHitBox().getMidPoint();
        MovecraftLocation midPointToPivot = new MovecraftLocation(
                midPoint.getX() - pivot.getX(),
                midPoint.getY() - pivot.getY(),
                midPoint.getZ() - pivot.getZ()
        );
        //Rotates both vectors
        MovecraftLocation rotatedOffSetToPivot = MathUtils.rotateVec(rotation, offSetToPivot);
        MovecraftLocation rotatedMidPointToPivot = MathUtils.rotateVec(rotation, midPointToPivot);
        //Subtracts rotatedOffSetToPivot - rotatedMidPointToPivot to get the new offset to the midpoint.
        offSet = new MovecraftLocation(
                rotatedOffSetToPivot.getX() - rotatedMidPointToPivot.getX(),
                rotatedOffSetToPivot.getY() - rotatedMidPointToPivot.getY(),
                rotatedOffSetToPivot.getZ() - rotatedMidPointToPivot.getZ()
        );
    }

    /**
     * Gets the stored location.
     * @return Returns the location.
     */
    public MovecraftLocation getAbsoluteLocation() {
        MovecraftLocation midPoint = craft.getHitBox().getMidPoint();
        return new MovecraftLocation(
                offSet.getX() + midPoint.getX(),
                offSet.getY() + midPoint.getY(),
                offSet.getZ() + midPoint.getZ()
        );
    }
}
