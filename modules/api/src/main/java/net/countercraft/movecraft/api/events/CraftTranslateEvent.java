package net.countercraft.movecraft.api.events;

import net.countercraft.movecraft.api.MovecraftLocation;
import net.countercraft.movecraft.api.craft.Craft;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;

/**
 * Called whenever a craft is translated.
 * This event is called before the craft is physically moved, but after collision is checked.
 * @see net.countercraft.movecraft.api.craft.Craft
 */
public class CraftTranslateEvent extends CraftEvent implements Cancellable {
    @NotNull private final MovecraftLocation newLocation;
    private boolean isCancelled = false;
    public CraftTranslateEvent(@NotNull Craft craft, @NotNull MovecraftLocation newLocation) {
        super(craft);
        this.newLocation = newLocation;
    }

    @NotNull
    public MovecraftLocation getNewLocation() {
        return newLocation;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        isCancelled=cancel;
    }
}
