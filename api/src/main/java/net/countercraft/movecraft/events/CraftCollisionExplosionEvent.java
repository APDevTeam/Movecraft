package net.countercraft.movecraft.events;

import net.countercraft.movecraft.craft.Craft;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class CraftCollisionExplosionEvent extends CraftEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    @NotNull private final World world;
    @NotNull private final Location location;
    private boolean cancelled;

    public CraftCollisionExplosionEvent(@NotNull Craft craft, @NotNull Location location, @NotNull World world) {
        super(craft);
        this.world = world;
        this.location = location;
        cancelled = false;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @NotNull
    public Location getLocation() {
        return location;
    }

    @NotNull
    public World getWorld() {
        return world;
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