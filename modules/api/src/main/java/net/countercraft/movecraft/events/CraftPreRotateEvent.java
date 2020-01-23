package net.countercraft.movecraft.events;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.Rotation;
import net.countercraft.movecraft.craft.Craft;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called before a craft is rotated
 * This event is called before the rotation algorithm, including collision
 * @see Craft
 */
public class CraftPreRotateEvent extends CraftEvent implements Cancellable {
    @NotNull private Rotation rotation;
    @NotNull private MovecraftLocation originPoint;
    @NotNull private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;
    public CraftPreRotateEvent(@NotNull Craft craft, @NotNull Rotation rotation, @NotNull MovecraftLocation originPoint) {
        super(craft, true);
        this.originPoint = originPoint;
        this.rotation = rotation;
    }

    @NotNull
    public Rotation getRotation() {
        return rotation;
    }

    public void setRotation(@NotNull Rotation rotation) {
        this.rotation = rotation;
    }

    @NotNull
    public MovecraftLocation getOriginPoint() {
        return originPoint;
    }

    public void setOriginPoint(@NotNull MovecraftLocation originPoint) {
        this.originPoint = originPoint;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    @NotNull
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
