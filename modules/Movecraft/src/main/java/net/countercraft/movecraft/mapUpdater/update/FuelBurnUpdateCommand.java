package net.countercraft.movecraft.mapUpdater.update;

import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.inventory.ItemStack;

public class FuelBurnUpdateCommand extends UpdateCommand {
    private final Block block;
    private final Craft craft;
    public FuelBurnUpdateCommand(Craft craft, Block furnace){
        this.block = furnace;
        this.craft = craft;
    }
    @Override
    public void doUpdate() {
        Furnace furnace = (Furnace) block.getState();
        for (Material fuel : Settings.FuelTypes.keySet()){
            if (furnace.getInventory().contains(fuel)){
                ItemStack item = furnace.getInventory().getItem(furnace.getInventory().first(fuel));
                int amount = item.getAmount();
                if (amount == 1) {
                    furnace.getInventory().remove(item);
                } else {
                    item.setAmount(amount - 1);
                }
                craft.setBurningFuel(craft.getBurningFuel() + Settings.FuelTypes.get(item.getType()));
            }
        }
    }
}
