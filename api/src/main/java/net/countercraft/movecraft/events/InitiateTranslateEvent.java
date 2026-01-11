package net.countercraft.movecraft.events;

import net.countercraft.movecraft.craft.Craft;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class InitiateTranslateEvent extends CraftEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    protected boolean cancelled = false;
    protected Vector translationDirection;

    public InitiateTranslateEvent(@NotNull Craft craft, @NotNull Vector vector) {
        super(craft);
        this.translationDirection = vector;
    }

    public Vector getTranslationDirection() {
        return this.translationDirection;
    }

    public void setTranslationDirection(Vector value) {
        this.translationDirection = value;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
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
