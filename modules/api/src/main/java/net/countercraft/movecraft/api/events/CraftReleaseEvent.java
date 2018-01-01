package net.countercraft.movecraft.api.events;

import net.countercraft.movecraft.api.craft.Craft;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called whenever a craft is released
 * @see net.countercraft.movecraft.api.craft.Craft
 */
@SuppressWarnings("unused")
public class CraftReleaseEvent extends CraftEvent{
    @NotNull private final Reason reason;
    private static final HandlerList HANDLERS = new HandlerList();

    public CraftReleaseEvent(@NotNull Craft craft, @NotNull Reason reason) {
        super(craft);
        this.reason = reason;
    }

    @NotNull
    public Reason getReason() {
        return reason;
    }

    public enum Reason{
        DISCONNECT,SUB_CRAFT,PLAYER,FORCE
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
