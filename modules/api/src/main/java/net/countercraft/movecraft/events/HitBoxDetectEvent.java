package net.countercraft.movecraft.events;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.utils.HitBox;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when the hitbox of a craft is detected
 */

public class HitBoxDetectEvent extends CraftEvent implements Cancellable {
    private boolean cancelled;
    @NotNull private final HitBox hitBox;
    @NotNull private String failMessage = "";
    @NotNull private static final HandlerList HANDLERS = new HandlerList();

    public HitBoxDetectEvent(@NotNull Craft craft, @NotNull HitBox hitBox) {
        super(craft);
        this.hitBox = hitBox;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

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

    /**
     * Gets the detected hitbox of a craft
     * @return the hitbox of the craft
     */
    @NotNull
    public HitBox getHitBox() {
        return hitBox;
    }

    @NotNull
    public String getFailMessage() {
        return failMessage;
    }

    public void setFailMessage(@NotNull String failMessage) {
        this.failMessage = failMessage;
    }
}
