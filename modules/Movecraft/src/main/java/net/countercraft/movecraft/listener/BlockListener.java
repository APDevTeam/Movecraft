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

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.utils.MathUtils;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.warfare.assault.Assault;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Hopper;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.material.Attachable;
import org.bukkit.material.MaterialData;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class BlockListener implements Listener {

    final int[] fragileBlocks = new int[]{26, 34, 50, 55, 63, 64, 65, 68, 69, 70, 71, 72, 75, 76, 77, 93, 94, 96, 131, 132, 143, 147, 148, 149, 150, 151, 171, 323, 324, 330, 331, 356, 404};
    private long lastDamagesUpdate = 0;

    @EventHandler
    public void onBlockPlace(final BlockPlaceEvent e) {
        if (!Settings.RestrictSiBsToRegions ||
                e.getBlockPlaced().getTypeId() != 54 ||
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
            ApplicableRegionSet regions = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(loc.getWorld()).getApplicableRegions(loc);
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
        if (e.getBlock().getType() == Material.WALL_SIGN) {
            Sign s = (Sign) e.getBlock().getState();
            if (s.getLine(0).equalsIgnoreCase(ChatColor.RED + "REGION DAMAGED!"))
                e.setCancelled(true);
            return;
        }
        if (Settings.ProtectPilotedCrafts) {
            MovecraftLocation mloc = MathUtils.bukkit2MovecraftLoc(e.getBlock().getLocation());
            if (CraftManager.getInstance().getCraftsInWorld(e.getBlock().getWorld()) == null) {
                return;
            }
            for (Craft craft : CraftManager.getInstance().getCraftsInWorld(e.getBlock().getWorld())) {
                if (craft == null || craft.getDisabled()) {
                    continue;
                }
                for (MovecraftLocation tloc : craft.getHitBox()) {
                    if (tloc.equals(mloc)) {
                        e.getPlayer().sendMessage(I18nSupport.getInternationalisedString("BLOCK IS PART OF A PILOTED CRAFT"));
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
        if (e.isCancelled() || CraftManager.getInstance().getCraftsInWorld(e.getLocation().getWorld()) == null) {
            return;
        }
        for (Craft tcraft : CraftManager.getInstance().getCraftsInWorld(e.getLocation().getWorld())) {
            if ((!tcraft.isNotProcessing()) && MathUtils.locationInHitbox(tcraft.getHitBox(), e.getLocation())) {
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
        if (block.getType() != Material.WATER &&
                block.getType() != Material.LAVA ||
                CraftManager.getInstance().getCraftsInWorld(block.getWorld()) == null) {
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
        if (CraftManager.getInstance().getCraftsInWorld(block.getWorld()) == null) {
            return;
        }
        for (Craft tcraft : CraftManager.getInstance().getCraftsInWorld(block.getWorld())) {
            MovecraftLocation mloc = new MovecraftLocation(block.getX(), block.getY(), block.getZ());
            if (MathUtils.locIsNearCraftFast(tcraft, mloc) &&
                    tcraft.getCruising() && (block.getTypeId() == 29 ||
                    block.getTypeId() == 33 || block.getTypeId() == 23 &&
                    !tcraft.isNotProcessing())) {
                event.setNewCurrent(event.getOldCurrent()); // don't allow piston movement on cruising crafts
                return;
            }
        }
    }

    // prevent pistons on cruising crafts
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPistonEvent(BlockPistonExtendEvent event) {
        Block block = event.getBlock();
        if (CraftManager.getInstance().getCraftsInWorld(block.getWorld()) == null) {
            return;
        }
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
        if (CraftManager.getInstance().getCraftsInWorld(block.getWorld()) == null) {
            return;
        }
        for (Craft tcraft : CraftManager.getInstance().getCraftsInWorld(block.getWorld())) {
            MovecraftLocation mloc = new MovecraftLocation(block.getX(), block.getY(), block.getZ());
            if (MathUtils.locIsNearCraftFast(tcraft, mloc) && tcraft.getCruising() && !tcraft.isNotProcessing()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    // prevent fragile items from dropping on cruising crafts
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPhysics(BlockPhysicsEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Block block = event.getBlock();

        final int[] fragileBlocks = new int[]{26, 34, 50, 55, 63, 64, 65, 68, 69, 70, 71, 72, 75, 76, 77, 93, 94, 96, 131, 132, 143, 147, 148, 149, 150, 151, 171, 193, 194, 195, 196, 197};
        if (CraftManager.getInstance().getCraftsInWorld(block.getWorld()) == null) {
            return;
        }
        for (Craft tcraft : CraftManager.getInstance().getCraftsInWorld(block.getWorld())) {
            MovecraftLocation mloc = new MovecraftLocation(block.getX(), block.getY(), block.getZ());
            if (!MathUtils.locIsNearCraftFast(tcraft, mloc)) {
                continue;
            }
            if (Arrays.binarySearch(fragileBlocks, block.getTypeId()) >= 0) {
                MaterialData m = block.getState().getData();
                BlockFace face = BlockFace.DOWN;
                boolean faceAlwaysDown = false;
                if (block.getTypeId() == 149 || block.getTypeId() == 150 || block.getTypeId() == 93 || block.getTypeId() == 94)
                    faceAlwaysDown = true;
                if (m instanceof Attachable && !faceAlwaysDown) {
                    face = ((Attachable) m).getAttachedFace();
                }
                if (!event.getBlock().getRelative(face).getType().isSolid()) {
//						if(event.getEventName().equals("BlockPhysicsEvent")) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockIgnite(BlockIgniteEvent event) {
        // replace blocks with fire occasionally, to prevent fast craft from simply ignoring fire
        if (!Settings.FireballPenetration ||
                event.isCancelled() ||
                event.getCause() != BlockIgniteEvent.IgniteCause.FIREBALL) {
            return;
        }
        Block testBlock = event.getBlock().getRelative(-1, 0, 0);
        if (!testBlock.getType().isBurnable())
            testBlock = event.getBlock().getRelative(1, 0, 0);
        if (!testBlock.getType().isBurnable())
            testBlock = event.getBlock().getRelative(0, 0, -1);
        if (!testBlock.getType().isBurnable())
            testBlock = event.getBlock().getRelative(0, 0, 1);

        if (!testBlock.getType().isBurnable()) {
            return;
        }
        // check to see if fire spread is allowed, don't check if worldguard integration is not enabled
        if (Movecraft.getInstance().getWorldGuardPlugin() != null && (Settings.WorldGuardBlockMoveOnBuildPerm || Settings.WorldGuardBlockSinkOnPVPPerm)) {
            ApplicableRegionSet set = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(testBlock.getWorld()).getApplicableRegions(testBlock.getLocation());
            if (!set.allows(DefaultFlag.FIRE_SPREAD)) {
                return;
            }
        }
        testBlock.setType(org.bukkit.Material.AIR);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockDispense(BlockDispenseEvent e) {
        if (CraftManager.getInstance().getCraftsInWorld(e.getBlock().getWorld()) == null) {
            return;
        }
        for (Craft craft : CraftManager.getInstance().getCraftsInWorld(e.getBlock().getWorld())) {
            if (craft != null &&
                    !craft.isNotProcessing() &&
                    MathUtils.locIsNearCraftFast(craft, MathUtils.bukkit2MovecraftLoc(e.getBlock().getLocation()))) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void explodeEvent(EntityExplodeEvent e) {
        // Remove any blocks from the list that were adjacent to water, to prevent spillage
        if (!Settings.DisableSpillProtection) {
            e.blockList().removeIf(b -> b.getY() > b.getWorld().getSeaLevel() &&
                    (b.getRelative(-1, 0, -1).isLiquid() ||
                            b.getRelative(-1, 1, -1).isLiquid() ||
                            b.getRelative(-1, 0, 0).isLiquid() ||
                            b.getRelative(-1, 1, 0).isLiquid() ||
                            b.getRelative(-1, 0, 1).isLiquid() ||
                            b.getRelative(-1, 1, 1).isLiquid() ||
                            b.getRelative(0, 0, -1).isLiquid() ||
                            b.getRelative(0, 1, -1).isLiquid() ||
                            b.getRelative(0, 0, 0).isLiquid() ||
                            b.getRelative(0, 1, 0).isLiquid() ||
                            b.getRelative(0, 0, 1).isLiquid() ||
                            b.getRelative(0, 1, 1).isLiquid() ||
                            b.getRelative(1, 0, -1).isLiquid() ||
                            b.getRelative(1, 1, -1).isLiquid() ||
                            b.getRelative(1, 0, 0).isLiquid() ||
                            b.getRelative(1, 1, 0).isLiquid() ||
                            b.getRelative(1, 0, 1).isLiquid() ||
                            b.getRelative(1, 1, 1).isLiquid()));
        }

        if (Settings.DurabilityOverride != null) {
            e.blockList().removeIf(b -> Settings.DurabilityOverride.containsKey(b.getTypeId()) &&
                    (new Random(b.getX() + b.getY() + b.getZ() + (System.currentTimeMillis() >> 12)))
                            .nextInt(100) < Settings.DurabilityOverride.get(b.getTypeId()));
        }
        processAssault(e);
        if (e.getEntity() == null)
            return;
        for (Player p : e.getEntity().getWorld().getPlayers()) {
            Entity tnt = e.getEntity();

            if (e.getEntityType() == EntityType.PRIMED_TNT && Settings.TracerRateTicks != 0) {
                long minDistSquared = 60 * 60;
                long maxDistSquared = Bukkit.getServer().getViewDistance() * 16;
                maxDistSquared = maxDistSquared - 16;
                maxDistSquared = maxDistSquared * maxDistSquared;
                // is the TNT within the view distance (rendered world) of the player, yet further than 60 blocks?
                if (p.getLocation().distanceSquared(tnt.getLocation()) < maxDistSquared && p.getLocation().distanceSquared(tnt.getLocation()) >= minDistSquared) {  // we use squared because its faster
                    final Location loc = tnt.getLocation();
                    final Player fp = p;
                    final World fw = e.getEntity().getWorld();
                    // then make a glowstone to look like the explosion, place it a little later so it isn't right in the middle of the volley
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            fp.sendBlockChange(loc, 89, (byte) 0);
                        }
                    }.runTaskLater(Movecraft.getInstance(), 5);
                    // then remove it
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            fp.sendBlockChange(loc, 0, (byte) 0);
                        }
                    }.runTaskLater(Movecraft.getInstance(), 160);
                }
            }
        }
    }

    //TODO: move to Warfare plugin
    private void processAssault(EntityExplodeEvent e){
        List<Assault> assaults = Movecraft.getInstance().getAssaultManager() != null ? Movecraft.getInstance().getAssaultManager().getAssaults() : null;
        if (assaults == null || assaults.size() == 0) {
            return;
        }
        WorldGuardPlugin worldGuard = Movecraft.getInstance().getWorldGuardPlugin();
        for (final Assault assault : assaults) {
            Iterator<Block> i = e.blockList().iterator();
            while (i.hasNext()) {
                Block b = i.next();
                if (b.getWorld() != assault.getWorld())
                    continue;
                ApplicableRegionSet regions = worldGuard.getRegionManager(b.getWorld()).getApplicableRegions(b.getLocation());
                boolean isInAssaultRegion = false;
                for (com.sk89q.worldguard.protection.regions.ProtectedRegion tregion : regions.getRegions()) {
                    if (assault.getRegionName().equals(tregion.getId())) {
                        isInAssaultRegion = true;
                    }
                }
                if (!isInAssaultRegion)
                    continue;
                // first see if it is outside the destroyable area
                com.sk89q.worldedit.Vector min = assault.getMinPos();
                com.sk89q.worldedit.Vector max = assault.getMaxPos();

                if (b.getLocation().getBlockX() < min.getBlockX() ||
                        b.getLocation().getBlockX() > max.getBlockX() ||
                        b.getLocation().getBlockZ() < min.getBlockZ() ||
                        b.getLocation().getBlockZ() > max.getBlockZ() ||
                        !Settings.AssaultDestroyableBlocks.contains(b.getTypeId()) ||
                        Arrays.binarySearch(fragileBlocks, b.getRelative(BlockFace.SOUTH).getTypeId()) >= 0 ||
                        Arrays.binarySearch(fragileBlocks, b.getRelative(BlockFace.DOWN).getTypeId()) >= 0 ||
                        Arrays.binarySearch(fragileBlocks, b.getRelative(BlockFace.UP).getTypeId()) >= 0 ||
                        Arrays.binarySearch(fragileBlocks, b.getRelative(BlockFace.EAST).getTypeId()) >= 0 ||
                        Arrays.binarySearch(fragileBlocks, b.getRelative(BlockFace.WEST).getTypeId()) >= 0 ||
                        Arrays.binarySearch(fragileBlocks, b.getRelative(BlockFace.NORTH).getTypeId()) >= 0) {
                    i.remove();
                }


                // whether or not you actually destroyed the block, add to damages
                long damages = assault.getDamages() + Settings.AssaultDamagesPerBlock;
                if (damages < assault.getMaxDamages()) {
                    assault.setDamages(damages);
                }

                // notify nearby players of the damages, do this 1 second later so all damages from this volley will be included
                if (System.currentTimeMillis() < lastDamagesUpdate + 4000) {
                    continue;
                }
                final Location floc = b.getLocation();
                final World fworld = b.getWorld();
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        long fdamages = assault.getDamages();
                        for (Player p : fworld.getPlayers()) {
                            if (Math.round(p.getLocation().getBlockX() / 1000.0) == Math.round(floc.getBlockX() / 1000.0) &&
                                    Math.round(p.getLocation().getBlockZ() / 1000.0) == Math.round(floc.getBlockZ() / 1000.0)) {
                                p.sendMessage("Damages: " + fdamages);
                            }
                        }
                    }
                }.runTaskLater(Movecraft.getInstance(), 20);
                lastDamagesUpdate = System.currentTimeMillis();

            }
        }

    }
}
