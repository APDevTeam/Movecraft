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
import net.countercraft.movecraft.craft.SinkingCraft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.events.FuelBurnEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.util.Tags;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
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

    protected boolean checkFuel() {
        // check for fuel, burn some from a furnace if needed. Blocks of coal are supported, in addition to coal and charcoal
        double fuelBurnRate = (double) craft.getType().getPerWorldProperty(CraftType.PER_WORLD_FUEL_BURN_RATE, craft.getWorld());
        if (fuelBurnRate == 0.0 || craft instanceof SinkingCraft)
            return true;

        if (craft.getBurningFuel() >= fuelBurnRate) {
            double burningFuel = craft.getBurningFuel();
            // call event
            final FuelBurnEvent event = new FuelBurnEvent(craft, burningFuel, fuelBurnRate);
            Bukkit.getPluginManager().callEvent(event);
            if (event.getBurningFuel() != burningFuel)
                burningFuel = event.getBurningFuel();
            if (event.getFuelBurnRate() != fuelBurnRate)
                fuelBurnRate = event.getFuelBurnRate();
            craft.setBurningFuel(burningFuel - fuelBurnRate);
            return true;
        }
        Block fuelHolder = null;

        var v = craft.getType().getObjectProperty(CraftType.FUEL_TYPES);
        if (!(v instanceof Map<?, ?>))
            throw new IllegalStateException("FUEL_TYPES must be of type Map");
        var fuelTypes = (Map<?, ?>) v;
        for (var e : fuelTypes.entrySet()) {
            if (!(e.getKey() instanceof Material))
                throw new IllegalStateException("Keys in FUEL_TYPES must be of type Material");
            if (!(e.getValue() instanceof Double))
                throw new IllegalStateException("Values in FUEL_TYPES must be of type Double");
        }

        for (MovecraftLocation bTest : craft.getHitBox()) {
            Block b = craft.getWorld().getBlockAt(bTest.getX(), bTest.getY(), bTest.getZ());
            if (Tags.FURNACES.contains(b.getType())) {
                InventoryHolder inventoryHolder = (InventoryHolder) b.getState();
                for (ItemStack stack : inventoryHolder.getInventory()) {
                    if (stack == null || !fuelTypes.containsKey(stack.getType()))
                        continue;
                    fuelHolder = b;
                    break;
                }
            }
        }
        if (fuelHolder == null) {
            return false;
        }
        InventoryHolder inventoryHolder = (InventoryHolder) fuelHolder.getState();
        for (ItemStack iStack : inventoryHolder.getInventory()) {
            if (iStack == null)
                continue;
            if (!fuelTypes.containsKey(iStack.getType()))
                continue;
            double burningFuel = (double) fuelTypes.get(iStack.getType());
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
            if (Tags.BUCKETS.contains(iStack.getType())) {
                //If buckets are accepted as fuel, replace with an empty bucket
                iStack.setType(Material.BUCKET);
            } else if (amount == minAmount) {
                inventoryHolder.getInventory().remove(iStack);
            } else if (amount < minAmount) {
                inventoryHolder.getInventory().remove(iStack);
                final ItemStack secStack = inventoryHolder.getInventory().getItem(inventoryHolder.getInventory().first(iStack.getType()));
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
