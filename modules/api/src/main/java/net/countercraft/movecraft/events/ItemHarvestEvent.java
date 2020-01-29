package net.countercraft.movecraft.events;


import net.countercraft.movecraft.craft.Craft;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Called when one or more blocks are harvested by a moving craft
 */
public class ItemHarvestEvent extends CraftEvent {
    private List<ItemStack> harvested;
    private static final HandlerList HANDLER_LIST = new HandlerList();
    public ItemHarvestEvent(@NotNull Craft craft, List<ItemStack> harvested) {
        super(craft, true);
        this.harvested = harvested;
    }


    public List<ItemStack> getHarvested() {
        return harvested;
    }

    public void setHarvested(List<ItemStack> harvested) {
        this.harvested = harvested;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
