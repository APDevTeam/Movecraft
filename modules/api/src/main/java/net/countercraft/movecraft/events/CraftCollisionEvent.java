package net.countercraft.movecraft.events;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import org.bukkit.World;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class CraftCollisionEvent extends CraftEvent{
    private static final HandlerList HANDLERS = new HandlerList();
    @NotNull private final HitBox hitBox;
    @NotNull private final World world;
    private boolean isCancelled = false;

    public CraftCollisionEvent(@NotNull Craft craft, @NotNull HitBox hitBox, @NotNull World world) {
        super(craft);
        this.hitBox = hitBox;
        this.world = world;
    }

    @NotNull
    public HitBox getHitBox() {
        return hitBox;
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
