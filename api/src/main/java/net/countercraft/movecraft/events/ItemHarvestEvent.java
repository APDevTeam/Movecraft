package net.countercraft.movecraft.events;


import net.countercraft.movecraft.craft.Craft;
import org.bukkit.Location;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Called when one or more blocks are harvested by a moving craft
 */
public class ItemHarvestEvent extends CraftEvent {
    @NotNull private List<ItemStack> harvested;
    private static final HandlerList HANDLER_LIST = new HandlerList();
    @NotNull private final Location location;
    public ItemHarvestEvent(@NotNull Craft craft, @NotNull List<ItemStack> harvested, @NotNull Location location) {
        super(craft);
        this.harvested = harvested;
        this.location = location;
    }


    @NotNull
    public List<ItemStack> getHarvested() {
        return harvested;
    }

    public void setHarvested(@NotNull List<ItemStack> harvested) {
        this.harvested = harvested;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @NotNull
    public Location getLocation() {
        return location;
    }
}
