package net.countercraft.movecraft.api.events;

import net.countercraft.movecraft.api.craft.Craft;
import org.jetbrains.annotations.NotNull;

/**
 * Called whenever a craft is piloted
 * @see net.countercraft.movecraft.api.craft.Craft
 */
public class CraftPilotEvent extends CraftEvent{
    @NotNull private final Reason reason;

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
}
