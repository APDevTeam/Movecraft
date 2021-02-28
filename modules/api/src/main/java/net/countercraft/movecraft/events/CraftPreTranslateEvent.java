package net.countercraft.movecraft.events;

import net.countercraft.movecraft.craft.Craft;
import org.bukkit.World;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called before a craft is translated
 * This event is called before the translation algorithm, including height limit checking and collision
 * @see Craft
 */
public class CraftPreTranslateEvent extends CraftEvent implements Cancellable {
    private int dx, dy, dz;
    @NotNull private World world;
    private boolean cancelled = false;
    @NotNull private String failMessage = "";
    private boolean playingFailSound = true;
    @NotNull private static final HandlerList HANDLERS = new HandlerList();
    public CraftPreTranslateEvent(@NotNull Craft craft, int dx, int dy, int dz, @NotNull World world) {
        super(craft);
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.world = world;
    }

    /**
     * Gets the translation change in X direction
     * @return translation change in X direction
     */
    public int getDx() {
        return dx;
    }

    /**
     * Sets the translation change in X direction
     * @param dx translation change in X direction
     */

    public void setDx(int dx) {
        this.dx = dx;
    }

    /**
     * Gets the translation change in Y direction
     * @return translation change in Y direction
     */
    public int getDy() {
        return dy;
    }

    /**
     * Sets the translation change in Y direction
     * @param dy translation change in Y direction
     */
    public void setDy(int dy) {
        this.dy = dy;
    }

    /**
     * Gets the translation change in Z direction
     * @return translation change in Z direction
     */
    public int getDz() {
        return dz;
    }

    /**
     * Sets the translation change in Z direction
     * @param dz translation change in Z direction
     */
    public void setDz(int dz) {
        this.dz = dz;
    }
    
    /**
     * Gets the destination world
     * @return world to translate to
     */
    @NotNull
    public World getWorld() {
        return world;
    }

    /**
     * Sets the destination world
     * @param world world to translate to
     */
    public void setWorld(@NotNull World world) {
        this.world = world;
    }

    @NotNull
    public String getFailMessage() {
        return failMessage;
    }

    public boolean isPlayingFailSound() {
        return playingFailSound;
    }

    public void setPlayingFailSound(boolean playingFailSound) {
        this.playingFailSound = playingFailSound;
    }

    public void setFailMessage(@NotNull String failMessage) {
        this.failMessage = failMessage;
    }

    @Override
    @NotNull
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
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
