package net.countercraft.movecraft.api.events;

import net.countercraft.movecraft.api.craft.Craft;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * A base event for all craft-related events
 * @see net.countercraft.movecraft.api.craft.Craft
 */
public abstract class CraftEvent extends Event{
    @NotNull protected final Craft craft;
    private static final HandlerList HANDLERS = new HandlerList();

    public CraftEvent(@NotNull Craft craft) {
        this.craft = craft;
    }

    @NotNull
    public final Craft getCraft(){
        return craft;
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
