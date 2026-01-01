package net.countercraft.movecraft;

import org.bukkit.World;
import org.bukkit.block.Furnace;
import org.bukkit.inventory.ItemStack;

public abstract class NMSHelper {

    public abstract boolean isFuel(ItemStack itemStack, World world);
    public abstract int getBurnDuration(ItemStack itemStack, World world);
    public abstract void setFurnaceBurnTime(int burnTime, int totalBurnTime, final Furnace furnace);

}
