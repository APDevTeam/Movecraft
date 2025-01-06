package net.countercraft.movecraft;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.util.MathUtils;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

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
        offSet = location.subtract(midPoint);
    }

    /**
     * Creates a new TrackedLocation instance which tracks a location about a craft's midpoint.
     * This additionally automatically adds the tracked location to the craft's list.
     * @param craft The craft that's that tied to the location.
     * @param location The absolute position to track. This location will be stored as a relative
     *                 location to the craft's central hitbox.
     * @param key The namespaced key that stores a set of tracked location within the craft.
     */
    public TrackedLocation(@NotNull Craft craft, @NotNull MovecraftLocation location, NamespacedKey key) {
        this(craft, location);
        if (craft.getTrackedLocations().get(key) == null) {
            craft.getTrackedLocations().put(key, new HashSet<>());
        }
        craft.getTrackedLocations().get(key).add(this);
    }

    /**
     * Rotates the stored location.
     * @param rotation A clockwise or counter-clockwise direction to rotate.
     */
    public void rotate(MovecraftRotation rotation, MovecraftLocation origin) {
        offSet = MathUtils.rotateVec(rotation, getAbsoluteLocation().subtract(origin));
    }

    /**
     * Gets the stored absolute location.
     * @return Returns the absolute location instead of a vector.
     */
    public MovecraftLocation getAbsoluteLocation() {
        MovecraftLocation midPoint = craft.getHitBox().getMidPoint();
        return offSet.add(midPoint);
    }

    /**
     * Gets the stored location as a position vector relative to the midpoint.
     * @return Returns the absolute location instead of a vector.
     */
    public MovecraftLocation getOffSet() {
        return offSet;
    }

    /**
     * Gets the tracked location's associated craft.
     * @return Returns the craft associated with the location.
     */
    public Craft getCraft() {
        return craft;
    }
}
