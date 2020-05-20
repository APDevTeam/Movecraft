package net.countercraft.movecraft.events;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.Rotation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.utils.HitBox;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called whenever a craft is rotated
 * This event is called before the craft is physically moved, but after collision is checked.
 * @see Craft
 */
@SuppressWarnings("unused")
public class CraftRotateEvent extends CraftEvent implements Cancellable {
    @NotNull private final HitBox oldHitBox;
    @NotNull private final HitBox newHitBox;
    @NotNull private String failMessage = "";
    @NotNull private final MovecraftLocation originPoint;
    @NotNull private final Rotation rotation;
    private boolean isCancelled = false;

    private static final HandlerList HANDLERS = new HandlerList();

    public CraftRotateEvent(@NotNull Craft craft, @NotNull Rotation rotation, @NotNull MovecraftLocation originPoint, @NotNull HitBox oldHitBox, @NotNull HitBox newHitBox) {
        super(craft, true);
        this.rotation = rotation;
        this.originPoint = originPoint;
        this.oldHitBox = oldHitBox;
        this.newHitBox = newHitBox;
    }

    @NotNull
    public Rotation getRotation() {
        return rotation;
    }

    @NotNull
    public MovecraftLocation getOriginPoint() {
        return originPoint;
    }

    @NotNull
    public HitBox getNewHitBox() {
        return newHitBox;
    }

    @NotNull
    public HitBox getOldHitBox(){
        return oldHitBox;
    }

    @NotNull
    public String getFailMessage() {
        return failMessage;
    }

    public void setFailMessage(@NotNull String failMessage) {
        this.failMessage = failMessage;
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
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        isCancelled = cancel;
    }
}
