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
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.warfare.assault.Assault;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class BlockListener implements Listener {

    final int[] fragileBlocks = new int[]{26, 34, 50, 55, 63, 64, 65, 68, 69, 70, 71, 72, 75, 76, 77, 93, 94, 96, 131, 132, 143, 147, 148, 149, 150, 151, 171, 323, 324, 330, 331, 356, 404};
    private long lastDamagesUpdate = 0;

    // TODO: Remove all references from SiB in movecraft and rely on StructureBoxes
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

    //TODO: move to Warfare plugin
    @EventHandler(priority = EventPriority.NORMAL)
    public void explodeEvent(EntityExplodeEvent e) {
        processAssault(e);
    }

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
                assault.setDamages(Math.min(damages, assault.getMaxDamages()));

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
                                p.sendMessage(I18nSupport.getInternationalisedString("Damage")+": " + fdamages);
                            }
                        }
                    }
                }.runTaskLater(Movecraft.getInstance(), 20);
                lastDamagesUpdate = System.currentTimeMillis();

            }
        }

    }
}