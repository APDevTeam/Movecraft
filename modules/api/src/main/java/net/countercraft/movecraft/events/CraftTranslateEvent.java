package net.countercraft.movecraft.events;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.utils.BitmapHitBox;
import net.countercraft.movecraft.utils.HashHitBox;

import org.bukkit.World;
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
    @NotNull private final BitmapHitBox oldHitBox;
    @NotNull private final BitmapHitBox newHitBox;
    @NotNull private final World world;
    @NotNull private String failMessage = "";
    private boolean playingFailSound = true;
    private boolean isCancelled = false;

    public CraftTranslateEvent(@NotNull Craft craft, @NotNull BitmapHitBox oldHitBox, @NotNull BitmapHitBox newHitBox, @NotNull World world) {
        super(craft, true);
        this.oldHitBox = oldHitBox;
        this.newHitBox = newHitBox;
        this.world = world;
    }

    @NotNull
    public BitmapHitBox getNewHitBox() {
        return newHitBox;
    }

    @NotNull
    public BitmapHitBox getOldHitBox(){
        return oldHitBox;
    }
    
    @NotNull
    public World getWorld() {
        return world;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        isCancelled=cancel;
    }

    public boolean isPlayingFailSound() {
        return playingFailSound;
    }

    public void setPlayingFailSound(boolean playingFailSound) {
        this.playingFailSound = playingFailSound;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public String getFailMessage() {
        return failMessage;
    }

    public void setFailMessage(@NotNull String failMessage) {
        this.failMessage = failMessage;
    }

    @SuppressWarnings("unused")
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
