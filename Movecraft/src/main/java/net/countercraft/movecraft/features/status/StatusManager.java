package net.countercraft.movecraft.features.status;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.datatag.CraftDataTagContainer;
import net.countercraft.movecraft.craft.datatag.CraftDataTagKey;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.countercraft.movecraft.features.status.events.CraftStatusUpdateEvent;
import net.countercraft.movecraft.processing.WorldManager;
import net.countercraft.movecraft.processing.effects.Effect;
import net.countercraft.movecraft.util.Counter;
import net.countercraft.movecraft.util.Tags;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Supplier;

public class StatusManager extends BukkitRunnable implements Listener {
    public static final CraftDataTagKey<Counter<Material>> CRAFT_MATERIALS = CraftDataTagContainer.tryRegisterTagKey(new NamespacedKey("movecraft", "materials"), craft -> new Counter<>());
    private static final CraftDataTagKey<Long> LAST_CHECK = CraftDataTagContainer.tryRegisterTagKey(new NamespacedKey("movecraft", "last-status-check"), craft -> System.currentTimeMillis());
    public static final CraftDataTagKey<Double> CRAFT_FUEL = CraftDataTagContainer.tryRegisterTagKey(new NamespacedKey("movecraft", "fuel"), craft -> 0D);

    @Override
    public void run() {
        for (Craft c : CraftManager.getInstance().getCrafts()) {
            long ticksElapsed = (System.currentTimeMillis() - c.getDataTag(LAST_CHECK)) / 50;
            if (ticksElapsed <= Settings.SinkCheckTicks)
                continue;

            WorldManager.INSTANCE.submit(new StatusUpdateTask(c));
        }
    }

    private static final class StatusUpdateTask implements Supplier<Effect> {
        private final Craft craft;
        private final Map<Material, Double> fuelTypes;

        private StatusUpdateTask(@NotNull Craft craft) {
            this.craft = craft;

            Object object = craft.getType().getObjectProperty(CraftType.FUEL_TYPES);
            if(!(object instanceof Map<?, ?> map))
                throw new IllegalStateException("FUEL_TYPES must be of type Map");
            for(var e : map.entrySet()) {
                if(!(e.getKey() instanceof Material))
                    throw new IllegalStateException("Keys in FUEL_TYPES must be of type Material");
                if(!(e.getValue() instanceof Double))
                    throw new IllegalStateException("Values in FUEL_TYPES must be of type Double");
            }
            fuelTypes = (Map<Material, Double>) map;
        }

        @Override
        public @NotNull Effect get() {
            Counter<Material> materials = new Counter<>();
            int totalNonNegligibleBlocks = 0;
            int totalNonNegligibleWaterBlocks = 0;
            double fuel = 0;
            for (MovecraftLocation l : craft.getHitBox()) {
                Material type = craft.getWorld().getBlockAt(l.getX(), l.getY(), l.getZ()).getType();
                materials.add(type);

                if (type != Material.FIRE && !type.isAir()) {
                    totalNonNegligibleBlocks++;
                }
                if (type != Material.FIRE && !type.isAir() && !Tags.FLUID.contains(type)) {
                    totalNonNegligibleWaterBlocks++;
                }

                if (Tags.FURNACES.contains(type)) {
                    InventoryHolder inventoryHolder = (InventoryHolder) craft.getWorld().getBlockAt(l.getX(), l.getY(), l.getZ()).getState();
                    for (ItemStack iStack : inventoryHolder.getInventory()) {
                        if (iStack == null || !fuelTypes.containsKey(iStack.getType()))
                            continue;
                        fuel += iStack.getAmount() * fuelTypes.get(iStack.getType());
                    }
                }
            }

            craft.setDataTag(CRAFT_MATERIALS, materials);
            craft.setDataTag(CRAFT_FUEL, fuel);
            Bukkit.getPluginManager().callEvent(new CraftStatusUpdateEvent(craft));

            return () -> {};
        }
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftStatsUpdate(@NotNull CraftStatusUpdateEvent e) {
        // TODO: Process disabled and sinking
    }
}
