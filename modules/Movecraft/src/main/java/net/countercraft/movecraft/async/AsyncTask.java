/*
 * This file is part of Movecraft.
 *
 *     Movecraft is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Movecraft is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Movecraft.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.countercraft.movecraft.async;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.FuelBurnEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

public abstract class AsyncTask extends BukkitRunnable {
    protected final Craft craft;

    protected AsyncTask(Craft c) {
        craft = c;
    }

    public void run() {
        try {
            execute();
            Movecraft.getInstance().getAsyncManager().submitCompletedTask(this);
        } catch (Exception e) {
            Movecraft.getInstance().getLogger().log(Level.SEVERE, I18nSupport.getInternationalisedString("Internal - Error - Processor thread encountered an error"));
            e.printStackTrace();
        }
    }

    protected abstract void execute() throws InterruptedException, ExecutionException;

    protected Craft getCraft() {
        return craft;
    }

    protected boolean checkFuel() throws ExecutionException, InterruptedException {
        // check for fuel, burn some from a furnace if needed. Blocks of coal are supported, in addition to coal and charcoal
        double fuelBurnRate = craft.getType().getFuelBurnRate(craft.getW());
        if (fuelBurnRate == 0.0 || craft.getSinking()) {
            return true;
        }
            if (craft.getBurningFuel() >= fuelBurnRate) {
                double burningFuel = craft.getBurningFuel();
                //call event
                final FuelBurnEvent event = new FuelBurnEvent(craft, burningFuel, fuelBurnRate);
                Bukkit.getPluginManager().callEvent(event);
                if (event.getBurningFuel() != burningFuel)
                    burningFuel = event.getBurningFuel();
                if (event.getFuelBurnRate() != fuelBurnRate)
                    fuelBurnRate = event.getFuelBurnRate();
                craft.setBurningFuel(burningFuel - fuelBurnRate);
                return true;
            }
            InventoryHolder fuelHolder = null;
            for (MovecraftLocation bTest : craft.getHitBox()) {
                Block b = craft.getWorld().getBlockAt(bTest.getX(), bTest.getY(), bTest.getZ());
                if (b.getType() != Material.FURNACE) {
                    continue;
                }
                fuelHolder = Bukkit.getScheduler().callSyncMethod(Movecraft.getInstance(), () -> {
                    InventoryHolder inventoryHolder = (InventoryHolder) b.getState();
                    for (ItemStack stack : inventoryHolder.getInventory()) {
                        if (stack == null || !craft.getType().getFuelTypes().containsKey(stack.getType()))
                            continue;
                        return inventoryHolder;
                    }
                    return null;
                }).get();

            }
            if (fuelHolder == null) {
                return false;
            }
            final Inventory inv = fuelHolder.getInventory();
            for (ItemStack iStack : inv) {
                if (iStack == null)
                    continue;
                if (!craft.getType().getFuelTypes().containsKey(iStack.getType()))
                    continue;
                double burningFuel = craft.getType().getFuelTypes().get(iStack.getType());
                //call event
                final FuelBurnEvent event = new FuelBurnEvent(craft, burningFuel, fuelBurnRate);
                Bukkit.getPluginManager().callEvent(event);
                if (event.getBurningFuel() != burningFuel)
                    burningFuel = event.getBurningFuel();
                if (event.getFuelBurnRate() != fuelBurnRate)
                    fuelBurnRate = event.getFuelBurnRate();
                if (burningFuel == 0.0) {
                    continue;
                }
                int amount = iStack.getAmount();
                int minAmount = 1;
                if (burningFuel < fuelBurnRate) {
                    minAmount = (int) fuelBurnRate;
                }
                if (iStack.getType() == Material.LAVA_BUCKET || iStack.getType() == Material.WATER_BUCKET || iStack.getType() == Material.MILK_BUCKET) {
                    //If water, lava or milk buckets are accepted as fuel, replace with an empty bucket
                    iStack.setType(Material.BUCKET);
                } else if (amount == minAmount) {
                    inv.remove(iStack);
                } else if (amount < minAmount) {
                    inv.remove(iStack);
                    final ItemStack secStack = inv.getItem(inv.first(iStack.getType()));
                    secStack.setAmount(secStack.getAmount() - (minAmount - amount));
                } else {
                    iStack.setAmount(amount - minAmount);
                }
                craft.setBurningFuel(craft.getBurningFuel() + burningFuel);
                break;
            }
            return true;



    }
}
