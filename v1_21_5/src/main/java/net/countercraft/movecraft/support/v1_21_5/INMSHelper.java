package net.countercraft.movecraft.support.v1_21_5;

import net.countercraft.movecraft.NMSHelper;
import net.minecraft.world.level.block.entity.FuelValues;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

public class INMSHelper extends NMSHelper {

    // TODO: Can be replaced with CraftItemType probably
    protected FuelValues getFuelValues(final World world) {
        return ((CraftWorld)world).getHandle().fuelValues();
    }

    @Override
    public boolean isFuel(ItemStack itemStack, World world) {
        return getFuelValues(world).isFuel(((CraftItemStack)itemStack).handle);
    }

    @Override
    public int getBurnDuration(ItemStack itemStack, World world) {
        if (isFuel(itemStack, world)) {
            return getFuelValues(world).burnDuration(((CraftItemStack)itemStack).handle);
        }
        return 0;
    }
}
