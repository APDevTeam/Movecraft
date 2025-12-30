package net.countercraft.movecraft.async;

import com.google.common.collect.Lists;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.TrackedLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.SinkingCraft;
import net.countercraft.movecraft.craft.SubCraft;
import net.countercraft.movecraft.craft.datatag.CraftDataTagKey;
import net.countercraft.movecraft.craft.datatag.CraftDataTagRegistry;
import net.countercraft.movecraft.craft.type.PropertyKeys;
import net.countercraft.movecraft.events.FuelBurnEvent;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.util.Tags;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Furnace;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public class FuelBurnRunnable implements Runnable {
    // FuelBurnRate: How much fuel gets burnt per tick?
    // "BurningFuel": How many ticks of fuel does the craft still have aboard?

    // TODO: Listen to furnace burn events (item consumption and stuff) and cancel them if it is a actively used furnace!

    public static final CraftDataTagKey<Boolean> IS_FUELED = CraftDataTagRegistry.INSTANCE.registerTagKey(new NamespacedKey(Movecraft.getInstance(), "is_fueled"), c -> false);
    // TODO: Replace with config object that has the values directly
    public static final CraftDataTagKey<ItemStack> CURRENT_FUEL_ITEM = CraftDataTagRegistry.INSTANCE.registerTagKey(new NamespacedKey(Movecraft.getInstance(), "fuel_item"), c -> ItemStack.empty());

    private static final NamespacedKey FURNACES_KEY = new NamespacedKey(Movecraft.getInstance(), "furnaces");
    public static final CraftDataTagKey<Set<TrackedLocation>> FURNACES = CraftDataTagRegistry.INSTANCE.registerTagKey(FURNACES_KEY, FuelBurnRunnable::calcFurnaceLocations);

    @Override
    public void run() {
        List<Craft> crafts = Lists.newArrayList(CraftManager.getInstance());
        for (Craft craft : crafts) {
            if (!doesBurnFuel(craft)) {
                continue;
            }
            // Burn current item or find a new one
            burnFuel(craft);

            boolean isFueled = craft.getDataTag(IS_FUELED);
            // Activate engines
            setEnginesActive(craft, isFueled);
        }
    }

    static double getFuelBurnRate(final Craft craft) {
        double fuelBurnRate = craft.getCraftProperties().get(PropertyKeys.FUEL_BURN_RATE, craft.getWorld());

        // Different fuel burn rate depending on gear and if the craft is moving
        boolean craftIsMoving = craft.getCruising();
        if (craftIsMoving) {
            // TODO: Customization on how much more fuel each gear uses!
            fuelBurnRate *= craft.getCurrentGear();
        } else {
            // Burns fuel 4 times slower
            fuelBurnRate /= 4.0D;
        }
        return fuelBurnRate;
    }

    static void burnFuel(final Craft craft) {
        // TODO: The more furnaces a craft has, the more fuel it should consume, but also the more furnaces, the faster it accelerates
        boolean isBurningFuel = false;
        double fuelBurnRate = getFuelBurnRate(craft);

        // Fuel item burning
        // We currently have somethign that we are burning
        if (craft.getBurningFuel() >= fuelBurnRate) {
            isBurningFuel = true;

            double burningFuel = craft.getBurningFuel();
            // call event
            final FuelBurnEvent event = new FuelBurnEvent(craft, burningFuel, fuelBurnRate);
            Bukkit.getPluginManager().callEvent(event);
            if (event.getBurningFuel() != burningFuel)
                burningFuel = event.getBurningFuel();
            if (event.getFuelBurnRate() != fuelBurnRate)
                fuelBurnRate = event.getFuelBurnRate();
            craft.setBurningFuel(burningFuel - fuelBurnRate);
        }
        // Find a new fuel item to burn, save to tag, remove item from furnace, throw event
        // We burnt the item we had, if we had any. Search for something new to burn
        else {
            // Prefer items in furnaces, consume one item from one furnace, remember it
            // If there was no item in the furnaces, draw it from fuel tanks
            Set<TrackedLocation> furnaces = craft.getDataTag(FURNACES);
            if (!furnaces.isEmpty()) {

                // TODO: Move into own method
                // region inventoryHolder function creation
                Function<TrackedLocation, FurnaceInventory> inventoryHolderFunction;
                if (Bukkit.isPrimaryThread()) {
                    final World world = craft.getWorld();
                    inventoryHolderFunction = (tl -> {
                        BlockState blockState = world.getBlockState(tl.getAbsoluteLocation().toBukkit(world));
                        if (Tags.FURNACES.contains(blockState.getType())) {
                            if (blockState instanceof InventoryHolder inventoryHolder) {
                                if (inventoryHolder.getInventory() instanceof FurnaceInventory) {
                                    return (FurnaceInventory) inventoryHolder.getInventory();
                                }
                            }
                        }
                        return null;
                    });
                } else {
                    final MovecraftWorld movecraftWorld = craft.getMovecraftWorld();
                    inventoryHolderFunction = (tl -> {
                        BlockState blockState = movecraftWorld.getState(tl.getAbsoluteLocation());
                        if (Tags.FURNACES.contains(blockState.getType())) {
                            if (blockState instanceof InventoryHolder inventoryHolder) {
                                if (inventoryHolder.getInventory() instanceof FurnaceInventory) {
                                    return (FurnaceInventory) inventoryHolder.getInventory();
                                }
                            }
                        }
                        return null;
                    });
                }
                // endregion inventoryHolder function creation

                // Search for the first furnace that exists and has fuel
                for (TrackedLocation trackedLocation : furnaces) {
                    FurnaceInventory furnaceInventory = inventoryHolderFunction.apply(trackedLocation);
                    if (furnaceInventory == null || furnaceInventory.isEmpty())
                        continue;

                    // Check fuel item
                    // If fueled, check for special effects of the cooked item
                    // If we consumed a bucket, add the bucket to the result slot or drop it in front of the furnace
                    ItemStack fuelItemStack = furnaceInventory.getFuel();
                    if (fuelItemStack == null || fuelItemStack.isEmpty())
                        continue;
                    NamespacedKey itemID = fuelItemStack.getType().getKey();
                    if (!craft.getCraftProperties().get(PropertyKeys.FUEL_TYPES).contains(itemID)) {
                        continue;
                    }
                    double burnTime = craft.getCraftProperties().get(PropertyKeys.FUEL_TYPES).get(itemID);
                    // TODO: Implement special effect API for top slot
                    final FuelBurnEvent fuelBurnEvent = new FuelBurnEvent(craft, burnTime, fuelBurnRate);
                    Bukkit.getPluginManager().callEvent(fuelBurnEvent);
                    burnTime = fuelBurnEvent.getBurningFuel();
                    fuelBurnRate = fuelBurnEvent.getFuelBurnRate();
                    if (burnTime <= 0)
                        continue;

                    // Calculate needed stacks
                    int stackSize = fuelItemStack.getAmount();
                    int comsumeQty = 1;
                    if (fuelBurnRate > burnTime) {
                        comsumeQty = (int) (fuelBurnRate / burnTime);
                        comsumeQty = Math.min(comsumeQty, stackSize);
                        burnTime *= comsumeQty;
                    }

                    craft.setDataTag(CURRENT_FUEL_ITEM, fuelItemStack.asQuantity(comsumeQty));

                    // TODO: Rewrite to support different stack sizes than 1 for buckets!
                    if (Tags.BUCKETS.contains(fuelItemStack.getType())) {
                        fuelItemStack.setType(Material.BUCKET);
                    }
                    else if (comsumeQty == stackSize) {
                        furnaceInventory.remove(fuelItemStack);
                    } else {
                        fuelItemStack.setAmount(stackSize - comsumeQty);
                    }

                    craft.setBurningFuel(craft.getBurningFuel() + burnTime);
                    craft.setMaxBurningFuel(craft.getBurningFuel());

                    isBurningFuel = true;

                    if (Settings.Debug) {
                        Movecraft.getInstance().getLogger().info("Active furnace: " + furnaceInventory.getHolder().getLocation());
                    }

                    break;
                }
            }

            if (!isBurningFuel) {
                // Search for fuel in fueltanks
            }
        }
        // TODO: Reset the furnace trackedlocations after a while

        craft.setDataTag(IS_FUELED, isBurningFuel);
    }

    static void setEnginesActive(final Craft craft, final boolean active) {
        short burnTime = 0;
        boolean setProgress = Movecraft.getInstance().getNMSHelper() != null;

        // Retrieve actual burn time from ServerLevel#fuelTypes() => Requires NMS or some other stuff
        if (setProgress) {
            final double burnPercentage = craft.getBurningFuel() / craft.getMaxBurningFuel();
            final ItemStack fuelItem = craft.getDataTag(CURRENT_FUEL_ITEM);
            if (Movecraft.getInstance().getNMSHelper().isFuel(fuelItem, craft.getWorld())) {
                double burnDuration = (Movecraft.getInstance().getNMSHelper().getBurnDuration(fuelItem, craft.getWorld()));
                burnTime = (short) (burnDuration * burnPercentage);
            }
        }

        for (TrackedLocation trackedLocation : craft.getDataTag(FURNACES)) {
            MovecraftLocation location = trackedLocation.getAbsoluteLocation();
            BlockData furnace;
            BlockState state;
            if (Bukkit.isPrimaryThread()) {
                furnace = craft.getWorld().getBlockData(location.toBukkit(craft.getWorld()));
                state = craft.getWorld().getBlockState(location.toBukkit(craft.getWorld()));
            } else {
                furnace = craft.getMovecraftWorld().getData(location);
                state = craft.getMovecraftWorld().getState(location);
            }
            if (state instanceof org.bukkit.block.Furnace furnace1) {
                if (active && setProgress) {
                    furnace1.setBurnTime(burnTime);
                }
            }
            if (furnace instanceof Furnace furnaceState) {
                furnaceState.setLit(active);
                state.setBlockData(furnaceState);
                state.update();
            }
        }
    }

    static Set<TrackedLocation> calcFurnaceLocations(final Craft craft) {
        Set<TrackedLocation> result = craft.getTrackedLocations().get(FURNACES_KEY);
        if (result != null) {
            return result;
        }
        result = new HashSet<>();

        Predicate<MovecraftLocation> testFurnacePredicate;
        if (Bukkit.isPrimaryThread()) {
            testFurnacePredicate = (ml) -> {
                return Tags.FURNACES.contains(craft.getWorld().getBlockAt(ml.toBukkit(craft.getWorld())).getType());
            };
        } else {
            testFurnacePredicate = (ml) -> {
                return Tags.FURNACES.contains(craft.getMovecraftWorld().getMaterial(ml));
            };
        }

        for (MovecraftLocation movecraftLocation : craft.getHitBox()) {
            if (testFurnacePredicate.test(movecraftLocation)) {
                result.add(new TrackedLocation(craft, movecraftLocation));
            }
        }

        return result;
    }

    static boolean doesBurnFuel(final Craft craft) {
        if (craft instanceof SinkingCraft) {
            return false;
        }
        if (craft instanceof SubCraft) {
            return false;
        }
        double fuelBurnRate = craft.getCraftProperties().get(PropertyKeys.FUEL_BURN_RATE, craft.getMovecraftWorld());
        return fuelBurnRate > 0.0D;
    }
}
