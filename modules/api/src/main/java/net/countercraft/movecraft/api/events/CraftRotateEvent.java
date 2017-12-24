package net.countercraft.movecraft.api.events;

import net.countercraft.movecraft.api.Rotation;
import net.countercraft.movecraft.api.craft.Craft;
import org.jetbrains.annotations.NotNull;

/**
 * Called whenever a craft is rotated
 * This event is called before the craft is physically moved, but after collision is checked.
 * @see net.countercraft.movecraft.api.craft.Craft
 */
@SuppressWarnings("unused")
public class CraftRotateEvent extends CraftEvent{
    @NotNull private final Rotation rotation;

    public CraftRotateEvent(@NotNull Craft craft, @NotNull Rotation rotation) {
        super(craft);
        this.rotation = rotation;
    }

    @NotNull
    public Rotation getRotation() {
        return rotation;
    }
}
