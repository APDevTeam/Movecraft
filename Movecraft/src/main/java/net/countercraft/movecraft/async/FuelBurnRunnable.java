package net.countercraft.movecraft.async;

import com.google.common.collect.Lists;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.TrackedLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.SinkingCraft;
import net.countercraft.movecraft.craft.SubCraft;
import net.countercraft.movecraft.craft.datatag.CraftDataTagKey;
import net.countercraft.movecraft.craft.datatag.CraftDataTagRegistry;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.events.FuelBurnEvent;
import net.countercraft.movecraft.util.Tags;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Furnace;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class FuelBurnRunnable implements Runnable {

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
            burnFuel(craft);

            boolean isFueled = craft.getDataTag(IS_FUELED);
            setEnginesActive(craft, isFueled);
        }
    }

    protected void burnFuel(final Craft craft) {
        boolean isBurningFuel = false;
        double fuelBurnRate = (double) craft.getType().getPerWorldProperty(CraftType.PER_WORLD_FUEL_BURN_RATE, craft.getWorld());

        // Different fuel burn rate depending on gear and if the craft is moving
        boolean craftIsMoving = craft.getCruising();
        if (craftIsMoving) {
            // TODO: Customization on how much more fuel each gear uses!
            fuelBurnRate *= craft.getCurrentGear();
        } else {
            // Burns fuel 4 times slower
            fuelBurnRate /= 4.0D;
        }

        // Fuel item burning
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
        else {

        }

        craft.setDataTag(IS_FUELED, isBurningFuel);
    }

    protected void setEnginesActive(final Craft craft, final boolean active) {
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
            if (furnace instanceof Furnace furnaceState) {
                furnaceState.setLit(active);
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
        double fuelBurnRate = (double) craft.getType().getPerWorldProperty(CraftType.PER_WORLD_FUEL_BURN_RATE, craft.getMovecraftWorld());
        return fuelBurnRate > 0.0D;
    }
}
