package net.countercraft.movecraft.events;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.utils.HitBox;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called whenever a craft is translated.
 * This event is called before the craft is physically moved, but after collision is checked.
 * @see Craft
 */
@SuppressWarnings("unused")
public class CraftTranslateEvent extends CraftEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    @NotNull private final HitBox oldHitBox;
    @NotNull private final HitBox newHitBox;
    private boolean isCancelled = false;

    public CraftTranslateEvent(@NotNull Craft craft, @NotNull HitBox oldHitBox, @NotNull HitBox newHitBox) {
        super(craft);
        this.oldHitBox = oldHitBox;
        this.newHitBox = newHitBox;
    }

    @NotNull
    public HitBox getNewHitBox() {
        return newHitBox;
    }

    @NotNull
    public HitBox getOldHitBox(){
        return oldHitBox;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        isCancelled=cancel;
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
