package net.countercraft.movecraft.events;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.utils.BitmapHitBox;

import org.bukkit.World;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class CraftCollisionEvent extends CraftEvent{
    private static final HandlerList HANDLERS = new HandlerList();
    @NotNull private final BitmapHitBox hitBox;
    @NotNull private final World world;
    private boolean isCancelled = false;

    public CraftCollisionEvent(@NotNull Craft craft, @NotNull BitmapHitBox hitBox, @NotNull World world) {
        super(craft);
        this.hitBox = hitBox;
        this.world = world;
    }

    @NotNull
    public BitmapHitBox getHitBox() {
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
