package net.countercraft.movecraft.events;

import net.countercraft.movecraft.Rotation;
import net.countercraft.movecraft.craft.Craft;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called whenever a craft is rotated
 * This event is called before the craft is physically moved, but after collision is checked.
 * @see Craft
 */
@SuppressWarnings("unused")
public class CraftRotateEvent extends CraftEvent{
    @NotNull private final Rotation rotation;
    private static final HandlerList HANDLERS = new HandlerList();

    public CraftRotateEvent(@NotNull Craft craft, @NotNull Rotation rotation) {
        super(craft);
        this.rotation = rotation;
    }

    @NotNull
    public Rotation getRotation() {
        return rotation;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @SuppressWarnings("unused")
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
