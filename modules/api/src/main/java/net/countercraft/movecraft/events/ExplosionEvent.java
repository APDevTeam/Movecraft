package net.countercraft.movecraft.events;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ExplosionEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Location explosionLocation;
    private final float explosionStrength;
    private boolean cancelled;

    public ExplosionEvent(Location explosionLocation, float explosionStrength) {
        this.explosionLocation = explosionLocation;
        this.explosionStrength = explosionStrength;
        cancelled = false;
    }

    public Location getExplosionLocation() {
        return explosionLocation;
    }

    public float getExplosionStrength() {
        return explosionStrength;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @SuppressWarnings("unused")
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}