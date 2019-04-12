package net.countercraft.movecraft.events;

import net.countercraft.movecraft.craft.Craft;
import org.bukkit.block.Block;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class CraftExplodeEvent extends CraftEvent {
    private static HandlerList handlerList = new HandlerList();
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
}
