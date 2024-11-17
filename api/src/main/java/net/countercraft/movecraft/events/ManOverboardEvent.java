package net.countercraft.movecraft.events;

import net.countercraft.movecraft.craft.Craft;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ManOverboardEvent extends CraftEvent implements Cancellable {
    @NotNull private static final HandlerList HANDLERS = new HandlerList();
    @NotNull private Location location;
    private boolean cancelled = false;

    public ManOverboardEvent(@NotNull Craft c, @NotNull Location location) {
        super(c);
        this.location = location;
    }

    @Override @NotNull
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @SuppressWarnings("unused")
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public void setLocation(@NotNull Location location) {
        this.location = location;
    }

    @NotNull
    public Location getLocation() {
        return location;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        this.cancelled = b;
    }
}
