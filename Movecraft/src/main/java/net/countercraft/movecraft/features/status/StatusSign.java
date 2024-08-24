package net.countercraft.movecraft.features.status;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.craft.type.RequiredBlockEntry;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.events.SignTranslateEvent;
import net.countercraft.movecraft.util.Counter;
import net.countercraft.movecraft.util.Tags;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

public final class StatusSign implements Listener {

    @EventHandler
    public void onCraftDetect(CraftDetectEvent event) {
        World world = event.getCraft().getWorld();
        for (MovecraftLocation location : event.getCraft().getHitBox()) {
            var block = location.toBukkit(world).getBlock();
            if (!Tag.SIGNS.isTagged(block.getType())) {
                continue;
            }
            BlockState state = block.getState();
            if (state instanceof Sign) {
                Sign sign = (Sign) state;
                if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Status:")) {
                    sign.setLine(1, "");
                    sign.setLine(2, "");
                    sign.setLine(3, "");
                    sign.update();
                }
            }
        }
    }

    @EventHandler
    public final void onSignTranslate(SignTranslateEvent event) {
        Craft craft = event.getCraft();
        if (!ChatColor.stripColor(event.getLine(0)).equalsIgnoreCase("Status:")) {
            return;
        }
        double fuel = craft.getDataTag(Craft.FUEL);

        int totalNonNegligibleBlocks = 0;
        int totalNonNegligibleWaterBlocks = 0;
        Counter<Material> materials = craft.getDataTag(Craft.MATERIALS);
        if (materials.isEmpty()) {
            return;
        }
        for (Material material : materials.getKeySet()) {
            if (material.equals(Material.FIRE) || material.isAir())
                continue;

            int add = materials.get(material);
            totalNonNegligibleBlocks += add;
            if (!Tags.WATER.contains(material)) {
                totalNonNegligibleWaterBlocks += add;
            }
        }
        //region Add flyblocks and moveblocks to displayBlocks
        Counter<RequiredBlockEntry> flyblocks = craft.getDataTag(Craft.FLYBLOCKS);
        Counter<RequiredBlockEntry> moveblocks = craft.getDataTag(Craft.MOVEBLOCKS);
        Counter<RequiredBlockEntry> displayBlocks = new Counter<>();

        displayBlocks.add(flyblocks);
        displayBlocks.add(moveblocks);
        //endregion
        int signLine = 1;
        int signColumn = 0;
        for (RequiredBlockEntry entry : displayBlocks.getKeySet()) {
            if (entry.getMin() == 0.0) {
                continue;
            }
            double percentPresent = (displayBlocks.get(entry) * 100D);
            if (craft.getType().getBoolProperty(CraftType.BLOCKED_BY_WATER)) {
                percentPresent /= totalNonNegligibleBlocks;
            } else {
                percentPresent /= totalNonNegligibleWaterBlocks;
            }
            String signText = "";
            if (percentPresent > entry.getMin() * 1.04) {
                signText += ChatColor.GREEN;
            } else if (percentPresent > entry.getMin() * 1.02) {
                signText += ChatColor.YELLOW;
            } else {
                signText += ChatColor.RED;
            }
            if (entry.getName() == null) {
                signText += entry.materialsToString().toUpperCase().charAt(0);
            } else {
                signText += entry.getName().toUpperCase().charAt(0);
            }
            signText += " ";
            signText += (int) percentPresent;
            signText += "/";
            signText += (int) entry.getMin();
            signText += "  ";
            if (signColumn == 0) {
                event.setLine(signLine, signText);
                signColumn++;
            } else if (signLine < 3) {
                String existingLine = event.getLine(signLine);
                existingLine += signText;
                event.setLine(signLine, existingLine);
                signLine++;
                signColumn = 0;
            }
        }

        String fuelText = "";
        int cruiseSkipBlocks = (int) craft.getType().getPerWorldProperty(CraftType.PER_WORLD_CRUISE_SKIP_BLOCKS, craft.getWorld());
        cruiseSkipBlocks++;
        double fuelBurnRate = (double) craft.getType().getPerWorldProperty(CraftType.PER_WORLD_FUEL_BURN_RATE, craft.getWorld());
        int fuelRange = (int) Math.round((fuel * (1 + cruiseSkipBlocks)) / fuelBurnRate);
        if (fuelRange > 1000) {
            fuelText += ChatColor.GREEN;
        } else if (fuelRange > 100) {
            fuelText += ChatColor.YELLOW;
        } else {
            fuelText += ChatColor.RED;
        }
        fuelText += "Fuel range:";
        fuelText += fuelRange;
        event.setLine(signLine, fuelText);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSignClickEvent(@NotNull PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (!(block.getState() instanceof Sign)) {
            return;
        }

        Sign sign = (Sign) block.getState();
        if (!ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Status:")) {
            return;
        }
        event.setCancelled(true);
    }
}