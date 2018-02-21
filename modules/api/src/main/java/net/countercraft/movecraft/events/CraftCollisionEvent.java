package net.countercraft.movecraft.events;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.utils.HitBox;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class CraftCollisionEvent extends CraftEvent{
    private static final HandlerList HANDLERS = new HandlerList();
    @NotNull private final HitBox hitBox;
    private boolean isCancelled = false;

    public CraftCollisionEvent(@NotNull Craft craft, @NotNull HitBox hitBox) {
        super(craft);
        this.hitBox = hitBox;
    }

    @NotNull
    public HitBox getHitBox() {
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
