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
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Hopper;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.material.Attachable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class BlockListener implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(@NotNull BlockBreakEvent e) {
        if (!Settings.ProtectPilotedCrafts)
            return;
        if (e.getBlock().getType() == Material.FIRE)
            return; // allow players to punch out fire

        MovecraftLocation loc = MathUtils.bukkit2MovecraftLoc(e.getBlock().getLocation());
        Craft craft = MathUtils.fastNearestCraftToLoc(CraftManager.getInstance().getCrafts(), e.getBlock().getLocation());
        if (craft == null || !craft.getHitBox().contains(loc) || craft.getDisabled())
            return;

        e.setCancelled(true);
    }

    //Prevents non pilots from placing blocks on your ship.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        if (!Settings.ProtectPilotedCrafts)
            return;

        MovecraftLocation loc = MathUtils.bukkit2MovecraftLoc(e.getBlockAgainst().getLocation());
        Craft craft = MathUtils.fastNearestCraftToLoc(CraftManager.getInstance().getCrafts(), e.getBlockAgainst().getLocation());
        if (craft == null || !craft.getHitBox().contains(loc) || craft.getDisabled() || !(craft instanceof PilotedCraft))
            return;

        Player p = e.getPlayer();
        if (((PilotedCraft) craft).getPilot() == p)
            return;

        e.setCancelled(true);
    }

    // prevent items from dropping from moving crafts
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemSpawn(@NotNull ItemSpawnEvent e) {
        if (e.isCancelled())
            return;

        MovecraftLocation loc = MathUtils.bukkit2MovecraftLoc(e.getLocation());
        Craft craft = MathUtils.fastNearestCraftToLoc(CraftManager.getInstance().getCrafts(), e.getLocation());
        if (craft == null || craft.isNotProcessing() || !craft.getHitBox().contains((loc)))
            return;

        e.setCancelled(true);
    }

    // process certain redstone on cruising crafts
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRedstoneEvent(@NotNull BlockRedstoneEvent e) {
        Block block = e.getBlock();
        MovecraftLocation loc = MathUtils.bukkit2MovecraftLoc(block.getLocation());
        Craft craft = MathUtils.fastNearestCraftToLoc(CraftManager.getInstance().getCrafts(), block.getLocation());
        if (craft == null || craft.isNotProcessing() || !craft.getHitBox().contains((loc)))
            return;

        if (block.getType() != Material.STICKY_PISTON || block.getType() != Material.PISTON || block.getType() != Material.DISPENSER)
            return;

        e.setNewCurrent(e.getOldCurrent()); // don't allow piston movement on cruising crafts
    }

    // prevent pistons on cruising crafts
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPistonEvent(@NotNull BlockPistonExtendEvent e) {
        Block block = e.getBlock();
        MovecraftLocation loc = MathUtils.bukkit2MovecraftLoc(block.getLocation());
        Craft craft = MathUtils.fastNearestCraftToLoc(CraftManager.getInstance().getCrafts(), block.getLocation());
        if (craft == null || !craft.getHitBox().contains((loc)))
            return;

        if (!craft.isNotProcessing())
            e.setCancelled(true);

        if (!craft.getType().getBoolProperty(CraftType.MERGE_PISTON_EXTENSIONS))
            return;

        BitmapHitBox hitBox = new BitmapHitBox();
        for (Block b : e.getBlocks()) {
            Vector dir = e.getDirection().getDirection();
            hitBox.add(new MovecraftLocation(b.getX() + dir.getBlockX(), b.getY() + dir.getBlockY(), b.getZ() + dir.getBlockZ()));
        }
        craft.setHitBox(craft.getHitBox().union(hitBox));
    }

    // prevent hoppers on cruising crafts
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHopperEvent(@NotNull InventoryMoveItemEvent e) {
        if (!(e.getSource().getHolder() instanceof Hopper))
            return;

        Hopper block = (Hopper) e.getSource().getHolder();
        MovecraftLocation loc = MathUtils.bukkit2MovecraftLoc(block.getLocation());
        Craft craft = MathUtils.fastNearestCraftToLoc(CraftManager.getInstance().getCrafts(), block.getLocation());
        if (craft == null || craft.isNotProcessing() || !craft.getHitBox().contains((loc)))
            return;

        e.setCancelled(true);
    }

    // prevent fragile items from dropping on cruising crafts
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPhysics(@NotNull BlockPhysicsEvent e) {
        if (e.isCancelled())
            return;
        Block block = e.getBlock();
        if (!Tags.FRAGILE_MATERIALS.contains(block.getType()))
            return;

        MovecraftLocation loc = MathUtils.bukkit2MovecraftLoc(block.getLocation());
        Craft craft = MathUtils.fastNearestCraftToLoc(CraftManager.getInstance().getCrafts(), block.getLocation());
        if (craft == null || !craft.getHitBox().contains((loc)))
            return;

        BlockData m = block.getBlockData();
        BlockFace face = BlockFace.DOWN;
        boolean faceAlwaysDown = block.getType() == Material.COMPARATOR || block.getType() == Material.REPEATER;
        if (m instanceof Attachable && !faceAlwaysDown)
            face = ((Attachable) m).getAttachedFace();

        if (e.getBlock().getRelative(face).getType().isSolid())
            return;

        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockDispense(@NotNull BlockDispenseEvent e) {
        MovecraftLocation loc = MathUtils.bukkit2MovecraftLoc(e.getBlock().getLocation());
        Craft craft = MathUtils.fastNearestCraftToLoc(CraftManager.getInstance().getCrafts(), e.getBlock().getLocation());
        if (craft == null || craft.isNotProcessing() || !craft.getHitBox().contains((loc)))
            return;

        e.setCancelled(true);
    }

    @EventHandler
    public void onFlow(@NotNull BlockFromToEvent e) {
        if (Settings.DisableSpillProtection || e.isCancelled())
            return;
        Block block = e.getBlock();
        if (!Tags.FLUID.contains(block.getType()))
            return;

        MovecraftLocation loc = MathUtils.bukkit2MovecraftLoc(block.getLocation());
        MovecraftLocation toLoc = MathUtils.bukkit2MovecraftLoc(e.getToBlock().getLocation());
        Craft craft = MathUtils.fastNearestCraftToLoc(CraftManager.getInstance().getCrafts(), e.getBlock().getLocation());
        if (craft == null || !craft.getHitBox().contains((loc)) || craft.getFluidLocations().contains(toLoc))
            return;

        e.setCancelled(true);
    }

    @EventHandler
    public void onIceForm(@NotNull BlockFormEvent e) {
        if (e.isCancelled() || !Settings.DisableIceForm)
            return;
        if (Tags.WATER.contains(e.getBlock().getType()))
            return;

        MovecraftLocation loc = MathUtils.bukkit2MovecraftLoc(e.getBlock().getLocation());
        Craft craft = MathUtils.fastNearestCraftToLoc(CraftManager.getInstance().getCrafts(), e.getBlock().getLocation());
        if (craft == null || !craft.getHitBox().contains((loc)))
            return;

        e.setCancelled(true);
    }
}
