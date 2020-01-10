package net.countercraft.movecraft.events;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.utils.HashHitBox;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class CraftCollisionEvent extends CraftEvent{
    private static final HandlerList HANDLERS = new HandlerList();
    @NotNull private final HashHitBox hitBox;
    private boolean isCancelled = false;

    public CraftCollisionEvent(@NotNull Craft craft, @NotNull HashHitBox hitBox) {
        super(craft);
        this.hitBox = hitBox;
    }

    public CraftCollisionEvent(@NotNull Craft craft, @NotNull HashHitBox hitBox, boolean isAsync) {
        super(craft, isAsync);
        this.hitBox = hitBox;
    }

    @NotNull
    public HashHitBox getHitBox() {
        return hitBox;
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
