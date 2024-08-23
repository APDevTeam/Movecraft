package net.countercraft.movecraft.features.status;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.SinkingCraft;
import net.countercraft.movecraft.craft.datatag.CraftDataTagKey;
import net.countercraft.movecraft.craft.datatag.CraftDataTagRegistry;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.craft.type.RequiredBlockEntry;
import net.countercraft.movecraft.features.status.events.CraftStatusUpdateEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.processing.WorldManager;
import net.countercraft.movecraft.processing.effects.Effect;
import net.countercraft.movecraft.util.Counter;
import net.countercraft.movecraft.util.Tags;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
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
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Supplier;

public class StatusManager extends BukkitRunnable implements Listener {
    private static final CraftDataTagKey<Long> LAST_STATUS_CHECK = CraftDataTagRegistry.INSTANCE.registerTagKey(new NamespacedKey("movecraft", "last-status-check"), craft -> System.currentTimeMillis());

    @Override
    public void run() {
        for (Craft c : CraftManager.getInstance().getCrafts()) {
            long ticksElapsed = (System.currentTimeMillis() - c.getDataTag(LAST_STATUS_CHECK)) / 50;
            if (ticksElapsed <= Settings.SinkCheckTicks)
                continue;

            c.setDataTag(LAST_STATUS_CHECK, System.currentTimeMillis());
            WorldManager.INSTANCE.submit(new StatusUpdateTask(c));
        }
    }

    private static final class StatusUpdateTask implements Supplier<Effect> {
        private final Craft craft;
        private final CraftType crafttype;
        private final Map<Material, Double> fuelTypes;

        private StatusUpdateTask(@NotNull Craft craft) {
            this.craft = craft;
            this.crafttype = craft.getType();

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
        public @Nullable Effect get() {
            Counter<Material> materials = new Counter<>();
            Counter<Material> flyblocks = new Counter<>();
            Counter<Material> moveblocks = new Counter<>();
            int nonNegligibleBlocks = 0;
            int nonNegligibleSolidBlocks = 0;
            double fuel = 0;
            for (MovecraftLocation l : craft.getHitBox()) {
                Material type = craft.getMovecraftWorld().getMaterial(l);
                materials.add(type);

                for(RequiredBlockEntry entry : crafttype.getRequiredBlockProperty(CraftType.FLY_BLOCKS)) {
                    if(entry.contains(type)) {
                        flyblocks.add(type);
                        break;
                    }
                }

                for(RequiredBlockEntry entry : crafttype.getRequiredBlockProperty(CraftType.MOVE_BLOCKS)) {
                    if(entry.contains(type)) {
                        moveblocks.add(type);
                        break;
                    }
                }

                if (type != Material.FIRE && !type.isAir()) {
                    nonNegligibleBlocks++;
                }
                if (type != Material.FIRE && !type.isAir() && !Tags.FLUID.contains(type)) {
                    nonNegligibleSolidBlocks++;
                }

                if (Tags.FURNACES.contains(type)) {
                    InventoryHolder inventoryHolder = (InventoryHolder) craft.getMovecraftWorld().getState(l);
                    for (ItemStack iStack : inventoryHolder.getInventory()) {
                        if (iStack == null || !fuelTypes.containsKey(iStack.getType()))
                            continue;
                        fuel += iStack.getAmount() * fuelTypes.get(iStack.getType());
                    }
                }
            }

            craft.setDataTag(Craft.FUEL, fuel);
            craft.setDataTag(Craft.MATERIALS, materials);
            craft.setDataTag(Craft.FLYBLOCKS, flyblocks);
            craft.setDataTag(Craft.MOVEBLOCKS, moveblocks);
            craft.setDataTag(Craft.NON_NEGLIGIBLE_BLOCKS, nonNegligibleBlocks);
            craft.setDataTag(Craft.NON_NEGLIGIBLE_SOLID_BLOCKS, nonNegligibleSolidBlocks);
            craft.setDataTag(LAST_STATUS_CHECK, System.currentTimeMillis());
            return () -> Bukkit.getPluginManager().callEvent(new CraftStatusUpdateEvent(craft));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftStatusUpdate(@NotNull CraftStatusUpdateEvent e) {
        Craft craft = e.getCraft();
        if (craft instanceof SinkingCraft)
            return;
        if (craft.getType().getDoubleProperty(CraftType.SINK_PERCENT) == 0.0)
            return;

        boolean sinking = false;
        boolean disabled = false;
        Counter<Material> materials = craft.getDataTag(Craft.MATERIALS);
        int nonNegligibleBlocks = craft.getDataTag(Craft.NON_NEGLIGIBLE_BLOCKS);
        int nonNegligibleSolidBlocks = craft.getDataTag(Craft.NON_NEGLIGIBLE_SOLID_BLOCKS);

        // Build up counters of the fly and move blocks
        Counter<RequiredBlockEntry> flyBlocks = new Counter<>();
        flyBlocks.putAll(craft.getType().getRequiredBlockProperty(CraftType.FLY_BLOCKS));
        Counter<RequiredBlockEntry> moveBlocks = new Counter<>();
        moveBlocks.putAll(craft.getType().getRequiredBlockProperty(CraftType.MOVE_BLOCKS));
        for (Material m : materials.getKeySet()) {
            for (RequiredBlockEntry entry : flyBlocks.getKeySet()) {
                if(entry.contains(m))
                    flyBlocks.add(entry, materials.get(m));
            }
            for (RequiredBlockEntry entry : moveBlocks.getKeySet()) {
                if(entry.contains(m))
                    moveBlocks.add(entry, materials.get(m));
            }
        }

        // now see if any of the resulting percentages are below the threshold specified in sinkPercent
        double sinkPercent = craft.getType().getDoubleProperty(CraftType.SINK_PERCENT) / 100.0;
        for (RequiredBlockEntry entry : flyBlocks.getKeySet()) {
            if(!entry.check(flyBlocks.get(entry), nonNegligibleBlocks, sinkPercent))
                sinking = true;
        }
        for (RequiredBlockEntry entry : moveBlocks.getKeySet()) {
            if (!entry.check(moveBlocks.get(entry), nonNegligibleBlocks, sinkPercent))
                disabled = true;
        }

        // And check the OverallSinkPercent
        if (craft.getType().getDoubleProperty(CraftType.OVERALL_SINK_PERCENT) != 0.0) {
            double percent;
            if (craft.getType().getBoolProperty(CraftType.BLOCKED_BY_WATER)) {
                percent = (double) nonNegligibleBlocks
                        / (double) craft.getOrigBlockCount();
            }
            else {
                percent = (double) nonNegligibleSolidBlocks
                        / (double) craft.getOrigBlockCount();
            }
            if (percent * 100.0 < craft.getType().getDoubleProperty(CraftType.OVERALL_SINK_PERCENT))
                sinking = true;
        }

        if (nonNegligibleBlocks == 0)
            sinking = true;

        // If the craft is disabled, play a sound and disable it.
        if (disabled && !craft.getDisabled()) {
            craft.setDisabled(true);
            craft.getAudience().playSound(Sound.sound(Key.key("entity.iron_golem.death"), Sound.Source.NEUTRAL, 5.0f, 5.0f));
        }

        // If the craft is sinking, let the player know and sink the craft.
        if (sinking) {
            craft.getAudience().sendMessage(I18nSupport.getInternationalisedComponent("Player - Craft is sinking"));
            craft.setCruising(false);
            CraftManager.getInstance().sink(craft);
        }
    }
}
