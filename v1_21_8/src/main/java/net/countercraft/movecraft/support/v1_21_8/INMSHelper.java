package net.countercraft.movecraft.support.v1_21_8;

import net.countercraft.movecraft.NMSHelper;
import net.countercraft.movecraft.util.ReflectUtils;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.FuelValues;
import org.bukkit.World;
import org.bukkit.block.Furnace;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.CraftBlockEntityState;
import org.bukkit.craftbukkit.block.CraftFurnace;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

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

    static final @NotNull Field FURNACE_LIT_TOTAL_TIME;
    static final @NotNull Field CRAFT_BLOCK_ENTITY_STATE_SNAPSHOT;

    static {
        try {
            FURNACE_LIT_TOTAL_TIME = ReflectUtils.getField(AbstractFurnaceBlockEntity.class, "litTotalTime");
            CRAFT_BLOCK_ENTITY_STATE_SNAPSHOT = ReflectUtils.getField(CraftBlockEntityState.class, "snapshot");
            CRAFT_BLOCK_ENTITY_STATE_SNAPSHOT.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setFurnaceBurnTime(int burnTime, int totalBurnTime, Furnace furnace) {
        Object snapshot = null;
        try {
            snapshot = CRAFT_BLOCK_ENTITY_STATE_SNAPSHOT.get(((CraftFurnace)furnace));
        } catch (Exception exception) {
            exception.printStackTrace();
            return;
        }
        if (snapshot == null) {
            return;
        }
        if (!(snapshot instanceof AbstractFurnaceBlockEntity)) {
            return;
        }

        AbstractFurnaceBlockEntity furnaceBlockEntity = (AbstractFurnaceBlockEntity) snapshot;
        try {
            FURNACE_LIT_TOTAL_TIME.set(furnaceBlockEntity, totalBurnTime);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        furnaceBlockEntity.litTimeRemaining = burnTime;
    }
}
