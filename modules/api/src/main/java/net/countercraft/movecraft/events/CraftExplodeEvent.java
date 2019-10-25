package net.countercraft.movecraft.events;

import net.countercraft.movecraft.craft.Craft;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class CraftExplodeEvent extends CraftEvent implements Cancellable {
    private static HandlerList handlerList = new HandlerList();
    private boolean cancelled = false;
    private double explosionForce;
    private Set<Block> destroyedBlocks;
    public CraftExplodeEvent(@NotNull Craft craft) {
        super(craft);
    }


    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList(){
        return handlerList;
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
