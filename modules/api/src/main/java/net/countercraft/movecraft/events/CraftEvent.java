package net.countercraft.movecraft.events;

import net.countercraft.movecraft.craft.Craft;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

/**
 * A base event for all craft-related events
 * @see Craft
 */
public abstract class CraftEvent extends Event{
    @NotNull protected final Craft craft;

    public CraftEvent(@NotNull Craft craft) {
        this.craft = craft;
    }

    public CraftEvent(@NotNull Craft craft, boolean isAsync){
        super(isAsync);
        this.craft = craft;
    }

    @NotNull
    public final Craft getCraft(){
        return craft;
    }
}
