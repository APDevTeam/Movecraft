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

package net.countercraft.movecraft.listener;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.LegacyUtils;
import net.countercraft.movecraft.utils.MathUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Hopper;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.material.PistonBaseMaterial;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BlockListener implements Listener {

    @EventHandler
    public void onBlockPlace(final BlockPlaceEvent e) {
        if (!Settings.RestrictSiBsToRegions ||
                e.getBlockPlaced().getType() != Material.CHEST ||
                !e.getItemInHand().hasItemMeta() ||
                !e.getItemInHand().getItemMeta().hasLore()) {
            return;
        }
        List<String> loreList = e.getItemInHand().getItemMeta().getLore();
        for (String lore : loreList) {
            if (!lore.contains("SiB")) {
                continue;
            }
            if (lore.toLowerCase().contains("merchant") || lore.toLowerCase().contains("mm")) {
                return;
            }
            Location loc = e.getBlockPlaced().getLocation();
            ApplicableRegionSet regions;
            if (Settings.IsLegacy){
                regions = LegacyUtils.getApplicableRegions(LegacyUtils.getRegionManager(Movecraft.getInstance().getWorldGuardPlugin(), loc.getWorld()), loc);//.getApplicableRegions();
            } else {
                regions = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(loc.getWorld())).getApplicableRegions(BlockVector3.at(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
            }
            if (regions.size() == 0) {
                e.getPlayer().sendMessage(I18nSupport.getInternationalisedString("SIB MUST BE PLACED IN REGION"));
                e.setCancelled(true);
                break;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(final BlockBreakEvent e) {
        if (e.isCancelled()) {
            return;
        }
        if (e.getBlock().getType().name().endsWith("WALL_SIGN")) {
            Sign s = (Sign) e.getBlock().getState();
            if (s.getLine(0).equalsIgnoreCase(ChatColor.RED + "REGION DAMAGED!")) {
                e.setCancelled(true);
                return;
            }
        }
        if (Settings.ProtectPilotedCrafts) {
            MovecraftLocation mloc = MathUtils.bukkit2MovecraftLoc(e.getBlock().getLocation());
            CraftManager.getInstance().getCraftsInWorld(e.getBlock().getWorld());
            for (Craft craft : CraftManager.getInstance().getCraftsInWorld(e.getBlock().getWorld())) {
                if (craft == null || craft.getDisabled()) {
                    continue;
                }
                for (MovecraftLocation tloc : craft.getHitBox()) {
                    if (tloc.equals(mloc)) {
                        e.getPlayer().sendMessage(I18nSupport.getInternationalisedString("Player - Block part of piloted craft"));
                        e.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    // prevent items from dropping from moving crafts
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemSpawn(final ItemSpawnEvent e) {
        if (e.isCancelled()) {
            return;
        }
        for (Craft tcraft : CraftManager.getInstance().getCraftsInWorld(e.getLocation().getWorld())) {
            if ((!tcraft.isNotProcessing()) && MathUtils.locationInHitBox(tcraft.getHitBox(), e.getLocation())) {
                e.setCancelled(true);
                return;
            }
        }
    }

    // prevent water and lava from spreading on moving crafts
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockFromTo(BlockFromToEvent e) {
        if (e.isCancelled()) {
            return;
        }
        Block block = e.getToBlock();
        if (block.getType() != Material.WATER && block.getType() != Material.LAVA) {
            return;
        }
        for (Craft tcraft : CraftManager.getInstance().getCraftsInWorld(block.getWorld())) {
            if ((!tcraft.isNotProcessing()) && MathUtils.locIsNearCraftFast(tcraft, MathUtils.bukkit2MovecraftLoc(block.getLocation()))) {
                e.setCancelled(true);
                return;
            }
        }
    }

    // process certain redstone on cruising crafts
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRedstoneEvent(BlockRedstoneEvent event) {
        Block block = event.getBlock();
        CraftManager.getInstance().getCraftsInWorld(block.getWorld());
        for (Craft tcraft : CraftManager.getInstance().getCraftsInWorld(block.getWorld())) {
            MovecraftLocation mloc = new MovecraftLocation(block.getX(), block.getY(), block.getZ());
            if (Settings.IsLegacy) {
                if (MathUtils.locIsNearCraftFast(tcraft, mloc) &&
                        tcraft.getCruising() && (block.getType() == LegacyUtils.PISTON_STICKY_BASE ||
                        block.getType() == LegacyUtils.PISTON_BASE || block.getType() == Material.DISPENSER &&
                        !tcraft.isNotProcessing())) {
                    event.setNewCurrent(event.getOldCurrent()); // don't allow piston movement on cruising crafts
                    return;
                }
            } else {
                if (MathUtils.locIsNearCraftFast(tcraft, mloc) &&
                        tcraft.getCruising() && (block.getType() == Material.STICKY_PISTON ||
                        block.getType() == Material.PISTON || block.getType() == Material.DISPENSER &&
                        !tcraft.isNotProcessing())) {
                    event.setNewCurrent(event.getOldCurrent()); // don't allow piston movement on cruising crafts
                    return;
                }
            }
        }
    }

    public void onPistonRetract(BlockPistonRetractEvent event){

    }
    // prevent pistons on cruising crafts
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPistonEvent(BlockPistonExtendEvent event) {
        Block block = event.getBlock();

        CraftManager.getInstance().getCraftsInWorld(block.getWorld());
        for (Craft tcraft : CraftManager.getInstance().getCraftsInWorld(block.getWorld())) {
            MovecraftLocation mloc = new MovecraftLocation(block.getX(), block.getY(), block.getZ());
            if (MathUtils.locIsNearCraftFast(tcraft, mloc) && tcraft.getCruising() && !tcraft.isNotProcessing()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    // prevent hoppers on cruising crafts
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHopperEvent(InventoryMoveItemEvent event) {
        if (!(event.getSource().getHolder() instanceof Hopper)) {
            return;
        }
        Hopper block = (Hopper) event.getSource().getHolder();
        CraftManager.getInstance().getCraftsInWorld(block.getWorld());
        for (Craft tcraft : CraftManager.getInstance().getCraftsInWorld(block.getWorld())) {
            MovecraftLocation mloc = new MovecraftLocation(block.getX(), block.getY(), block.getZ());
            if (MathUtils.locIsNearCraftFast(tcraft, mloc) && tcraft.getCruising() && !tcraft.isNotProcessing()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockDispense(BlockDispenseEvent e) {
        CraftManager.getInstance().getCraftsInWorld(e.getBlock().getWorld());
        for (Craft craft : CraftManager.getInstance().getCraftsInWorld(e.getBlock().getWorld())) {
            if (craft != null &&
                    !craft.isNotProcessing() &&
                    MathUtils.locIsNearCraftFast(craft, MathUtils.bukkit2MovecraftLoc(e.getBlock().getLocation()))) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onFlow(BlockFromToEvent e){
        if(Settings.DisableSpillProtection)
            return;
        if(!e.getBlock().isLiquid())
            return;
        MovecraftLocation loc = MathUtils.bukkit2MovecraftLoc(e.getBlock().getLocation());
        MovecraftLocation toLoc = MathUtils.bukkit2MovecraftLoc(e.getToBlock().getLocation());
        for(Craft craft : CraftManager.getInstance().getCraftsInWorld(e.getBlock().getWorld())){
            if(craft.getHitBox().contains((loc)) && !craft.getFluidLocations().contains(toLoc)) {
                e.setCancelled(true);
                break;
            }
        }
    }

    @Nullable
    private Craft adjacentCraft(@NotNull Location location) {
        for (Craft craft : CraftManager.getInstance().getCraftsInWorld(location.getWorld())) {
            if (!MathUtils.locIsNearCraftFast(craft, MathUtils.bukkit2MovecraftLoc(location))) {
                continue;
            }
            return craft;
        }
        return null;
    }

    private boolean pistonFacingLocation(Location loc) {
        final Vector[] SHIFTS = {new Vector(0,1,0), new Vector(0,-1,0),
                new Vector(1,0,0), new Vector(-1,0,0),
                new Vector(0,0,1), new Vector(0,0,-1)};
        for (Vector shift : SHIFTS) {
            final Location test = loc.add(shift);
            if (!(test.getBlock().getState().getData() instanceof PistonBaseMaterial)) {
                continue;
            }
            PistonBaseMaterial piston = (PistonBaseMaterial) test.getBlock().getState().getData();
            if (!test.getBlock().getRelative(piston.getFacing()).getLocation().equals(loc)) {
                continue;
            }
            return true;
        }
        return false;
    }
    private boolean isFragileBlock(Block block) {
        Material type = block.getType();
        BlockState state = block.getState();
        return type.name().endsWith("_BED") ||
                state instanceof Sign ||
                type.name().endsWith("DOOR") ||
                type.name().endsWith("BUTTON") ||
                type.name().endsWith("_PLATE") ||
                type == Material.REDSTONE_WIRE ||
                type.name().endsWith("TORCH") ||
                type == Material.TRIPWIRE ||
                type == Material.TRIPWIRE_HOOK ||
                type == Material.LADDER ||
                type == Material.LEVER ||
                type == Material.DAYLIGHT_DETECTOR ||
                (Settings.IsLegacy ?
                        (type == LegacyUtils.BED_BLOCK||
                        type == LegacyUtils.PISTON_EXTENSION||
                        type == LegacyUtils.SIGN_POST||
                        type == LegacyUtils.WOOD_DOOR||
                        type == LegacyUtils.WALL_SIGN||
                        type == LegacyUtils.IRON_DOOR_BLOCK||
                        type == LegacyUtils.REDSTONE_TORCH_OFF||
                        type == LegacyUtils.REDSTONE_TORCH_ON||
                        type == LegacyUtils.DIODE_BLOCK_OFF||
                        type == LegacyUtils.DIODE_BLOCK_ON||
                        type == LegacyUtils.TRAP_DOOR ||
                        type == LegacyUtils.REDSTONE_COMPARATOR_OFF||
                        type == LegacyUtils.REDSTONE_COMPARATOR_ON ||
                        type == LegacyUtils.CARPET||
                        type == LegacyUtils.DAYLIGHT_DETECTOR_INVERTED)
        :
                        (type == Material.REPEATER ||
                        type == Material.COMPARATOR )
        );

    }
}

