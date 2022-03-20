package net.countercraft.movecraft.events;

import net.countercraft.movecraft.craft.Craft;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called whenever a craft is released
 * @see Craft
 */
@SuppressWarnings("unused")
public class CraftReleaseEvent extends CraftEvent implements Cancellable {
    @NotNull private final Reason reason;
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;

    public CraftReleaseEvent(@NotNull Craft craft, @NotNull Reason reason) {
        super(craft);
        this.reason = reason;
    }

    @NotNull
    public Reason getReason() {
        return reason;
    }

    public enum Reason{
        DISCONNECT,SUB_CRAFT,PLAYER,FORCE,EMPTY,SUNK,REPAIR,DEATH,INACTIVE
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
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
