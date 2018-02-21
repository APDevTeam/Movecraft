package net.countercraft.movecraft.events;

import net.countercraft.movecraft.craft.Craft;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called whenever a craft is piloted
 * @see Craft
 */
@SuppressWarnings("unused")
public class CraftPilotEvent extends CraftEvent{
    @NotNull private final Reason reason;
    private static final HandlerList HANDLERS = new HandlerList();

    public CraftPilotEvent(@NotNull Craft craft, @NotNull Reason reason){
        super(craft);
        this.reason = reason;
    }

    @NotNull
    public Reason getReason() {
        return reason;
    }

    public enum Reason{
        SUB_CRAFT,PLAYER,FORCE
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
