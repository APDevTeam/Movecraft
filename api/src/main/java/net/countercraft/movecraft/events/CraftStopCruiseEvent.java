package net.countercraft.movecraft.events;

import net.countercraft.movecraft.craft.Craft;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class CraftStopCruiseEvent extends CraftEvent {
    @NotNull private final CraftStopCruiseEvent.Reason reason;
    private static final HandlerList HANDLERS = new HandlerList();

    public CraftStopCruiseEvent(@NotNull Craft craft, @NotNull CraftStopCruiseEvent.Reason reason) {
        super(craft);
        this.reason = reason;
    }

    @NotNull
    public CraftStopCruiseEvent.Reason getReason() {
        return reason;
    }

    public enum Reason{
        OUT_OF_FUEL,
        CRAFT_SUNK,
        CRAFT_RELEASE,
        SIGN_INTERACTION,
        COMMAND,
        UNKNOWN
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
