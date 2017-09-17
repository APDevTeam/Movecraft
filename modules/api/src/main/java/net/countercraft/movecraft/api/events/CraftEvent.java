package net.countercraft.movecraft.api.events;

import net.countercraft.movecraft.api.craft.Craft;
import org.jetbrains.annotations.NotNull;

/**
 * A base event for all craft-related events
 * @see net.countercraft.movecraft.api.craft.Craft
 */
public abstract class CraftEvent {
    @NotNull protected final Craft craft;

    public CraftEvent(@NotNull Craft craft) {
        this.craft = craft;
    }

    @NotNull
    public final Craft getCraft(){
        return craft;
    }
}
