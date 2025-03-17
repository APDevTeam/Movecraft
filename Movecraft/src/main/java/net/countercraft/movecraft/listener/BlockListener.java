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

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.PilotedCraft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.util.MathUtils;
import net.countercraft.movecraft.util.Tags;
import net.countercraft.movecraft.util.hitboxes.BitmapHitBox;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Hopper;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.material.Attachable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import java.util.List;

public class BlockListener implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(@NotNull BlockBreakEvent e) {
        if (!Settings.ProtectPilotedCrafts)
            return;
        if (e.getBlock().getType() == Material.FIRE)
            return; // allow players to punch out fire

        Location location = e.getBlock().getLocation();
        MovecraftLocation loc = MathUtils.bukkit2MovecraftLoc(location);
        for (Craft craft : MathUtils.craftsNearLocFast(CraftManager.getInstance().getCrafts(), location)) {
            if (craft.getDisabled() || !craft.getHitBox().contains(loc))
                continue;

            e.setCancelled(true);
            return;
        }
    }

    //Prevents non pilots from placing blocks on your ship.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        if (!Settings.ProtectPilotedCrafts)
            return;

        Location location = e.getBlockAgainst().getLocation();
        MovecraftLocation loc = MathUtils.bukkit2MovecraftLoc(location);
        Player p = e.getPlayer();
        for (Craft craft : MathUtils.craftsNearLocFast(CraftManager.getInstance().getCrafts(), location)) {
            if (craft.getDisabled() || !(craft instanceof PilotedCraft) || !craft.getHitBox().contains(loc))
                continue;
            if (((PilotedCraft) craft).getPilot() == p)
                continue;

            e.setCancelled(true);
            return;
        }
    }

    // prevent items from dropping from moving crafts
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemSpawn(@NotNull ItemSpawnEvent e) {
        Location location = e.getLocation();
        MovecraftLocation loc = MathUtils.bukkit2MovecraftLoc(location);
        for (Craft craft : MathUtils.craftsNearLocFast(CraftManager.getInstance().getCrafts(), location)) {
            if (craft.isNotProcessing() || !craft.getHitBox().contains(loc))
                continue;

            e.setCancelled(true);
            return;
        }
    }

    // process certain redstone on cruising crafts
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRedstoneEvent(@NotNull BlockRedstoneEvent e) {
        Block block = e.getBlock();
        if (block.getType() != Material.STICKY_PISTON || block.getType() != Material.PISTON || block.getType() != Material.DISPENSER)
            return;

        Location location = block.getLocation();
        MovecraftLocation loc = MathUtils.bukkit2MovecraftLoc(location);
        for (Craft craft : MathUtils.craftsNearLocFast(CraftManager.getInstance().getCrafts(), location)) {
            if (craft.isNotProcessing() || !craft.getHitBox().contains(loc))
                continue;

            e.setNewCurrent(e.getOldCurrent()); // don't allow piston movement on cruising crafts
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtendEvent(@NotNull BlockPistonExtendEvent e) {
        onPistonEvent(e, e.getBlocks());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetractEvent(@NotNull BlockPistonRetractEvent e) {
        onPistonEvent(e, e.getBlocks());
    }

    public void onPistonEvent(@NotNull BlockPistonEvent e, final @NotNull List<Block> affectedBlocks) {
        Block block = e.getBlock();
        Location location = block.getLocation();
        MovecraftLocation loc = MathUtils.bukkit2MovecraftLoc(location);
        for (Craft craft : MathUtils.craftsNearLocFast(CraftManager.getInstance().getCrafts(), location)) {
            if (!craft.getHitBox().contains(loc))
                continue;

           if (!craft.isNotProcessing())
               e.setCancelled(true); // prevent pistons on cruising crafts           
            // merge piston extensions to craft if the property is true
           if (!craft.getType().getBoolProperty(CraftType.MERGE_PISTON_EXTENSIONS))
                continue;

           BitmapHitBox hitBox = new BitmapHitBox();
           for (Block b : affectedBlocks) {
               Vector dir = e.getDirection().getDirection();
               hitBox.add(new MovecraftLocation(b.getX() + dir.getBlockX(), b.getY() + dir.getBlockY(), b.getZ() + dir.getBlockZ()));
           }
           craft.setHitBox(craft.getHitBox().union(hitBox));
        }
    }


    // prevent hoppers on cruising crafts
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHopperEvent(@NotNull InventoryMoveItemEvent e) {
        if (!(e.getSource().getHolder() instanceof Hopper))
            return;

        Hopper block = (Hopper) e.getSource().getHolder();
        Location location = block.getLocation();
        MovecraftLocation loc = MathUtils.bukkit2MovecraftLoc(location);
        for (Craft craft : MathUtils.craftsNearLocFast(CraftManager.getInstance().getCrafts(), location)) {
            if (craft.isNotProcessing() || !craft.getHitBox().contains(loc))
                continue;

            e.setCancelled(true);
            return;
        }
    }

    // prevent fragile items from dropping on cruising crafts
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPhysics(@NotNull BlockPhysicsEvent e) {
        Block block = e.getBlock();
        if (!Tags.FRAGILE_MATERIALS.contains(block.getType()))
            return;

        Location location = block.getLocation();
        MovecraftLocation loc = MathUtils.bukkit2MovecraftLoc(location);
        for (Craft craft : MathUtils.craftsNearLocFast(CraftManager.getInstance().getCrafts(), location)) {
            if (!craft.getHitBox().contains(loc))
                continue;
        
            BlockData m = block.getBlockData();
            BlockFace face = BlockFace.DOWN;
            boolean faceAlwaysDown = block.getType() == Material.COMPARATOR || block.getType() == Material.REPEATER;
            if (m instanceof Attachable && !faceAlwaysDown)
                face = ((Attachable) m).getAttachedFace();

            if (e.getBlock().getRelative(face).getType().isSolid())
                continue;

            e.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDispense(@NotNull BlockDispenseEvent e) {
        Location location = e.getBlock().getLocation();
        MovecraftLocation loc = MathUtils.bukkit2MovecraftLoc(location);
        for (Craft craft : MathUtils.craftsNearLocFast(CraftManager.getInstance().getCrafts(), location)) {
            if (craft.isNotProcessing() || !craft.getHitBox().contains(loc))
                continue;

            e.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFlow(@NotNull BlockFromToEvent e) {
        if (Settings.DisableSpillProtection)
            return;
        Block block = e.getBlock();
        if (!Tags.FLUID.contains(block.getType()) && (!(block.getBlockData() instanceof Waterlogged waterlogged) || !waterlogged.isWaterlogged()))
            return; // If the source is not a fluid or waterlogged, exit

        Location location = block.getLocation();
        MovecraftLocation loc = MathUtils.bukkit2MovecraftLoc(location);
        MovecraftLocation toLoc = MathUtils.bukkit2MovecraftLoc(e.getToBlock().getLocation());
        for (Craft craft : MathUtils.craftsNearLocFast(CraftManager.getInstance().getCrafts(), location)) {
            if (!craft.getHitBox().contains(loc) || craft.getFluidLocations().contains(toLoc))
                continue;

            e.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onIceForm(@NotNull BlockFormEvent e) {
        if (!Settings.DisableIceForm)
            return;
        if (Tags.WATER.contains(e.getBlock().getType()))
            return;

        Location location = e.getBlock().getLocation();
        MovecraftLocation loc = MathUtils.bukkit2MovecraftLoc(location);
        for (Craft craft : MathUtils.craftsNearLocFast(CraftManager.getInstance().getCrafts(), location)) {
            if (!craft.getHitBox().contains(loc))
                continue;

            e.setCancelled(true);
            return;
        }
    }
}
