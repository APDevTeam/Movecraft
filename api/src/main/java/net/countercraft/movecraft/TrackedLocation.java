package net.countercraft.movecraft;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.util.MathUtils;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.HashSet;

public class TrackedLocation {

    private MovecraftLocation vector;

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
        Craft craft = this.getCraft();
        this.vector = location.subtract(craft.getCraftOrigin());
    }

    /**
     * Creates a new TrackedLocation instance which tracks a location about a craft's midpoint.
     * @param craft The craft that's that tied to the location.
     * @param location The absolute position to track. This location will be stored as a relative
     *                 location to the craft's central hitbox.
     * @param key The namespaced key that references a set of tracked locations stored within the craft.
     */
    public TrackedLocation(@NotNull Craft craft, @NotNull MovecraftLocation location, @NotNull NamespacedKey key) {
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
    public void rotate(MovecraftRotation rotation) {
        this.vector = MathUtils.rotateVec(rotation, this.vector);
    }

    /**
     * Gets the stored absolute location.
     * @return Returns the absolute location instead of a vector.
     */
    public MovecraftLocation getAbsoluteLocation() {
        Craft craft = this.getCraft();
        return this.vector.add(craft.getCraftOrigin());
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

    /**
     * Gets the craft associated with the tracked location.
     * @return Returns the craft.
     */
    public Craft getCraft() {
        if (this.craft.get() == null) {
            throw new RuntimeException("Craft of tracked location is null! This indicates that the craft object was destroyed but somehow the tracked location is still around!");
        }
        return this.craft.get();
    }
}
