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
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.api.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.api.craft.CraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.api.MathUtils;
import net.countercraft.movecraft.api.MovecraftLocation;
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
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.material.Attachable;
import org.bukkit.material.MaterialData;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class BlockListener implements Listener {

    final int[] fragileBlocks = new int[]{26, 34, 50, 55, 63, 64, 65, 68, 69, 70, 71, 72, 75, 76, 77, 93, 94, 96, 131, 132, 143, 147, 148, 149, 150, 151, 171, 323, 324, 330, 331, 356, 404};
    private long lastDamagesUpdate = 0;

    @EventHandler
    public void onBlockPlace(final BlockPlaceEvent e) {
        if (Settings.RestrictSiBsToRegions) {
            if (e.getBlockPlaced().getTypeId() == 54) {
                if (e.getItemInHand().hasItemMeta()) {
                    if (e.getItemInHand().getItemMeta().hasLore()) {
                        List<String> loreList = e.getItemInHand().getItemMeta().getLore();
                        for (String lore : loreList) {
                            if (lore.contains("SiB")) {
                                boolean isMM = false;
                                if (lore.toLowerCase().contains("merchant") || lore.toLowerCase().contains("mm")) {
                                    isMM = true;
                                }
                                Location loc = e.getBlockPlaced().getLocation();
                                ApplicableRegionSet regions = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(loc.getWorld()).getApplicableRegions(loc);
                                if (regions.size() == 0 && !isMM) {
                                    e.getPlayer().sendMessage(I18nSupport.getInternationalisedString("SIB MUST BE PLACED IN REGION"));
                                    e.setCancelled(true);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(final BlockBreakEvent e) {

        if (e.isCancelled()) {
            return;
        }
        if (Settings.ProtectPilotedCrafts) {
            MovecraftLocation mloc = MathUtils.bukkit2MovecraftLoc(e.getBlock().getLocation());
            boolean blockInCraft = false;
            if (CraftManager.getInstance().getCraftsInWorld(e.getBlock().getWorld()) != null)
                for (Craft craft : CraftManager.getInstance().getCraftsInWorld(e.getBlock().getWorld())) {
                    if (craft != null) {
                        if (!craft.getDisabled())
                            for (MovecraftLocation tloc : craft.getBlockList()) {
                                if (tloc.getX() == mloc.getX() && tloc.getY() == mloc.getY() && tloc.getZ() == mloc.getZ())
                                    blockInCraft = true;
                            }
                    }
                }
            if (blockInCraft) {
                e.getPlayer().sendMessage(I18nSupport.getInternationalisedString("BLOCK IS PART OF A PILOTED CRAFT"));
                e.setCancelled(true);
            }
        }
        if (e.getBlock().getType() == Material.WALL_SIGN) {
            Sign s = (Sign) e.getBlock().getState();
            if (s.getLine(0).equalsIgnoreCase(ChatColor.RED + "REGION DAMAGED!"))
                e.setCancelled(true);
        }
    }

    // prevent items from dropping from moving crafts
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemSpawn(final ItemSpawnEvent e) {
        if (e.isCancelled()) {
            return;
        }
        if (CraftManager.getInstance().getCraftsInWorld(e.getLocation().getWorld()) != null) {
            for (Craft tcraft : CraftManager.getInstance().getCraftsInWorld(e.getLocation().getWorld())) {
                if ((!tcraft.isNotProcessing()) && MathUtils.playerIsWithinBoundingPolygon(tcraft.getHitBox(), tcraft.getMinX(), tcraft.getMinZ(), MathUtils.bukkit2MovecraftLoc(e.getLocation()))) {
                    e.setCancelled(true);
                    return;
                }
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
        if (block.getType() == Material.WATER || block.getType() == Material.LAVA) {
            if (CraftManager.getInstance().getCraftsInWorld(block.getWorld()) != null) {
                for (Craft tcraft : CraftManager.getInstance().getCraftsInWorld(block.getWorld())) {
                    if ((!tcraft.isNotProcessing()) && MathUtils.locIsNearCraftFast(tcraft, MathUtils.bukkit2MovecraftLoc(block.getLocation()))) {
                        e.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    // process certain redstone on cruising crafts
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRedstoneEvent(BlockRedstoneEvent event) {
        Block block = event.getBlock();
        if (CraftManager.getInstance().getCraftsInWorld(block.getWorld()) != null) {
            for (Craft tcraft : CraftManager.getInstance().getCraftsInWorld(block.getWorld())) {

                MovecraftLocation mloc = new MovecraftLocation(block.getX(), block.getY(), block.getZ());
                if (MathUtils.locIsNearCraftFast(tcraft, mloc) && tcraft.getCruising()) {
                    if ((block.getTypeId() == 29) || (block.getTypeId() == 33)) {
                        event.setNewCurrent(event.getOldCurrent()); // don't allow piston movement on cruising crafts
                        return;
                    }

                    if ((block.getTypeId() == 23) && (!tcraft.isNotProcessing())) {
                        event.setNewCurrent(event.getOldCurrent()); // don't activate dispensers while craft is reconstructing
                        return;
                    }
                }
            }
        }
    }

    // prevent pistons on cruising crafts
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPistonEvent(BlockPistonExtendEvent event) {
        Block block = event.getBlock();
        if (!Settings.AllowPistonExtendOnCruisingCrafts) {
        	if (CraftManager.getInstance().getCraftsInWorld(block.getWorld()) != null) {
        		for (Craft tcraft : CraftManager.getInstance().getCraftsInWorld(block.getWorld())) {

        			MovecraftLocation mloc = new MovecraftLocation(block.getX(), block.getY(), block.getZ());
        			if (MathUtils.locIsNearCraftFast(tcraft, mloc)) {
        				if (tcraft.getCruising() || !tcraft.isNotProcessing()) {
        					event.setCancelled(true);
        				}
        			}
        		}
        	}
        }
    }

    // prevent hoppers on cruising crafts
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHopperEvent(InventoryMoveItemEvent event) {
        if (event.getSource().getHolder() instanceof Hopper) {
            Hopper block = (Hopper) event.getSource().getHolder();
            if (CraftManager.getInstance().getCraftsInWorld(block.getWorld()) != null) {
                for (Craft tcraft : CraftManager.getInstance().getCraftsInWorld(block.getWorld())) {

                    MovecraftLocation mloc = new MovecraftLocation(block.getX(), block.getY(), block.getZ());
                    if (MathUtils.locIsNearCraftFast(tcraft, mloc)) {
                        if (tcraft.getCruising() || !tcraft.isNotProcessing()) {
                            event.setCancelled(true);
                        }
                    }
                }
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
        if (CraftManager.getInstance().getCraftsInWorld(block.getWorld()) != null) {
            for (Craft tcraft : CraftManager.getInstance().getCraftsInWorld(block.getWorld())) {
                MovecraftLocation mloc = new MovecraftLocation(block.getX(), block.getY(), block.getZ());

/*				// on cruising crafts, movecraft will handle the repeater logic
                if(tcraft.isNotProcessing() && MathUtils.locIsNearCraftFast(tcraft, mloc) && tcraft.getCruising()) {
					if((block.getTypeId()==94) || (block.getTypeId()==93)) {
						int repeaterFacing=block.getData()&3;
						Block powerBlock=null;
						if(repeaterFacing==0) // find the block that should be powering this one
							powerBlock=block.getRelative(BlockFace.SOUTH);
						if(repeaterFacing==1)
							powerBlock=block.getRelative(BlockFace.WEST);
						if(repeaterFacing==2)
							powerBlock=block.getRelative(BlockFace.NORTH);
						if(repeaterFacing==3)
							powerBlock=block.getRelative(BlockFace.EAST);
						BlockTranslateCommand muc=null;
						// if the power source is on, but the repeater is off, power it on. If the source is off, but the repeater is on, power it off
						boolean sourcePowered=false;
						if(powerBlock.getBlockPower()>0)
							sourcePowered=true;
						if(powerBlock.getBlockPower()==0)
							sourcePowered=false;
						if(powerBlock.getTypeId()==93)
							sourcePowered=false;
						if((powerBlock.getTypeId()==94) && ((powerBlock.getData()&3)==repeaterFacing))
							sourcePowered=true;
						if((powerBlock.getTypeId()==77) || (powerBlock.getTypeId()==143))
							if(powerBlock.getData()<8)
								sourcePowered=false;
						if((sourcePowered) && (block.getTypeId()==93)) { // power source is on, but this repeater is off
							muc = new BlockTranslateCommand(mloc, 94, block.getData(), null);
						}
						if((!sourcePowered) && (block.getTypeId()==94)) { // power source is off, but this repeater is on
							muc = new BlockTranslateCommand(mloc, 93, block.getData(), null);
						}
						event.setCancelled(true);
						if(muc!=null) {
							HashMap<BlockTranslateCommand, Long> blockChanges=tcraft.getScheduledBlockChanges();
							Long timeToChange=System.currentTimeMillis();
							int repeaterTicks=block.getData()>>2;
							repeaterTicks++;
							timeToChange+=repeaterTicks*100;
							HashMap<BlockTranslateCommand, Long> scheduledChanges=tcraft.getScheduledBlockChanges();
							scheduledChanges.put(muc, timeToChange);
							tcraft.setScheduledBlockChanges(scheduledChanges);
							return;
						}
					}
				}*/
                if ( /*(!tcraft.isNotProcessing()) &&*/ (MathUtils.locIsNearCraftFast(tcraft, mloc))) {
                    boolean isFragile = (Arrays.binarySearch(fragileBlocks, block.getTypeId()) >= 0);
                    if (isFragile) {
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
        }
    }

    private CraftType getCraftTypeFromString(String s) {
        for (CraftType t : CraftManager.getInstance().getCraftTypes()) {
            if (s.equalsIgnoreCase(t.getCraftName())) {
                return t;
            }
        }

        return null;
    }

	/*@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockPhysicsEvent(BlockPhysicsEvent e) {
		Location loc=e.getBlock().getLocation();
		if(MapUpdateManager.getInstance().getProtectedBlocks().contains(loc))
			e.setCancelled(true);
	}
	
	//@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockRedstoneEvent(BlockRedstoneEvent e) {
		Location loc=e.getBlock().getLocation();
		if(MapUpdateManager.getInstance().getProtectedBlocks().contains(loc))
			e.setNewCurrent(0);
	}*/

    @EventHandler(priority = EventPriority.NORMAL)
    public void onSignChange(SignChangeEvent event) {
        Player p = event.getPlayer();
        if (p == null)
            return;
        String signText = org.bukkit.ChatColor.stripColor(event.getLine(0));
        // did the player try to create a craft command sign?
        if (getCraftTypeFromString(signText) != null) {
            if (!Settings.RequireCreatePerm) {
                return;
            }
            if (!p.hasPermission("movecraft." + ChatColor.stripColor(event.getLine(0)) + ".create")) {
                p.sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
                event.setCancelled(true);
            }
        }
        if (signText.equalsIgnoreCase("Cruise: OFF") || signText.equalsIgnoreCase("Cruise: ON")) {
            if (!p.hasPermission("movecraft.cruisesign") && Settings.RequireCreatePerm) {
                p.sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
                event.setCancelled(true);
            }
        }
        if (signText.equalsIgnoreCase("Crew:")) {
            String crewName = org.bukkit.ChatColor.stripColor(event.getLine(1));
            if (!p.getName().equalsIgnoreCase(crewName)) {
                p.sendMessage(I18nSupport.getInternationalisedString("You can only create a Crew: sign for yourself"));
                event.setLine(1, p.getName());
//				event.setCancelled(true);
            }
        }
        if (signText.equalsIgnoreCase("Pilot:")) {
            String pilotName = org.bukkit.ChatColor.stripColor(event.getLine(1));
            if (pilotName.isEmpty()) {
                event.setLine(1, p.getName());
//				event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (!Settings.FireballPenetration)
            return;
        if (event.isCancelled())
            return;
        // replace blocks with fire occasionally, to prevent fast craft from simply ignoring fire
        if (event.getCause() == BlockIgniteEvent.IgniteCause.FIREBALL) {
            Block testBlock = event.getBlock().getRelative(-1, 0, 0);
            if (!testBlock.getType().isBurnable())
                testBlock = event.getBlock().getRelative(1, 0, 0);
            if (!testBlock.getType().isBurnable())
                testBlock = event.getBlock().getRelative(0, 0, -1);
            if (!testBlock.getType().isBurnable())
                testBlock = event.getBlock().getRelative(0, 0, 1);

            if (testBlock.getType().isBurnable()) {
                boolean isBurnAllowed = true;
                // check to see if fire spread is allowed, don't check if worldguard integration is not enabled
                if (Movecraft.getInstance().getWorldGuardPlugin() != null && (Settings.WorldGuardBlockMoveOnBuildPerm || Settings.WorldGuardBlockSinkOnPVPPerm)) {
                    ApplicableRegionSet set = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(testBlock.getWorld()).getApplicableRegions(testBlock.getLocation());
                    if (!set.allows(DefaultFlag.FIRE_SPREAD)) {
                        isBurnAllowed = false;
                    }
                }
                if (isBurnAllowed)
                    testBlock.setType(org.bukkit.Material.AIR);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockDispense(BlockDispenseEvent e) {
        if (CraftManager.getInstance().getCraftsInWorld(e.getBlock().getWorld()) != null) {
            for (Craft craft : CraftManager.getInstance().getCraftsInWorld(e.getBlock().getWorld())) {
                if (craft != null) {
                    if (!craft.isNotProcessing()) {
                        if (MathUtils.locIsNearCraftFast(craft, MathUtils.bukkit2MovecraftLoc(e.getBlock().getLocation()))) {
                            e.setCancelled(true);
                            return;
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void explodeEvent(EntityExplodeEvent e) {
        // Remove any blocks from the list that were adjacent to water, to prevent spillage
        if (!Settings.DisableSpillProtection) {
            Iterator<Block> i = e.blockList().iterator();
            while (i.hasNext()) {
                Block b = i.next();
                boolean isNearWater = false;
                if (b.getY() > b.getWorld().getSeaLevel())
                    for (int mx = -1; mx <= 1; mx++)
                        for (int mz = -1; mz <= 1; mz++)
                            for (int my = 0; my <= 1; my++) {
                                if (b.getRelative(mx, my, mz).getType() == Material.STATIONARY_WATER || b.getRelative(mx, my, mz).getType() == Material.WATER || b.getRelative(mx, my, mz).getType() == Material.STATIONARY_LAVA || b.getRelative(mx, my, mz).getType() == Material.LAVA)
                                    isNearWater = true;
                            }
                if (isNearWater) {
                    i.remove();
                }
            }
        }

        if (Settings.DurabilityOverride != null) {
            Iterator<Block> bi = e.blockList().iterator();
            while (bi.hasNext()) {
                Block b = bi.next();
                if (Settings.DurabilityOverride.containsKey(b.getTypeId())) {
                    long seed = b.getX() + b.getY() + b.getZ() + (System.currentTimeMillis() >> 12);
                    Random ran = new Random(seed);
                    float chance = ran.nextInt(100);
                    if (chance < Settings.DurabilityOverride.get(b.getTypeId())) {
                        bi.remove();
                    }
                }
            }
        }

        List<Assault> assaults = Movecraft.getInstance().getAssaultManager().getAssaults();
        if (assaults.size() != 0) {
            WorldGuardPlugin worldGuard = Movecraft.getInstance().getWorldGuardPlugin();
            for (final Assault assault : assaults) {
                Iterator<Block> i = e.blockList().iterator();
                while (i.hasNext()) {
                    Block b = i.next();
                    if (b.getWorld() != assault.getWorld())
                        continue;
                    ApplicableRegionSet regions = worldGuard.getRegionManager(b.getWorld()).getApplicableRegions(b.getLocation());
                    boolean isInAssaultRegion = false;
                    String foundAssaultName = null;
                    for (com.sk89q.worldguard.protection.regions.ProtectedRegion tregion : regions.getRegions()) {
                        if (assault.getRegionName().equals(tregion.getId())) {
                            isInAssaultRegion = true;
                            foundAssaultName = tregion.getId();
                        }
                    }
                    if (!isInAssaultRegion)
                        continue;
                    // first see if it is outside the destroyable area
                    com.sk89q.worldedit.Vector min = assault.getMinPos();
                    com.sk89q.worldedit.Vector max = assault.getMaxPos();
                    boolean destroyBlock = true;
                    if (b.getLocation().getBlockX() < min.getBlockX() || b.getLocation().getBlockX() > max.getBlockX() || b.getLocation().getBlockZ() < min.getBlockZ() || b.getLocation().getBlockZ() > max.getBlockZ())
                        destroyBlock = false;

                    // easy stuff is done, from here its harder / slower. So check it every time to avoid redundant checks
                    if (destroyBlock) { //Now check to see if the block ID is on the list of destroyable blocks
                        int blockID = b.getTypeId();
                        if (!Settings.AssaultDestroyableBlocks.contains(blockID)) {
                            destroyBlock = false;
                        }
                    }
                    if (destroyBlock) { // does it have an attached block that might break?
                        if (Arrays.binarySearch(fragileBlocks, b.getRelative(BlockFace.SOUTH).getTypeId()) >= 0) {
                            destroyBlock = false;
                        } else if (Arrays.binarySearch(fragileBlocks, b.getRelative(BlockFace.DOWN).getTypeId()) >= 0) {
                            destroyBlock = false;
                        } else if (Arrays.binarySearch(fragileBlocks, b.getRelative(BlockFace.UP).getTypeId()) >= 0) {
                            destroyBlock = false;
                        } else if (Arrays.binarySearch(fragileBlocks, b.getRelative(BlockFace.EAST).getTypeId()) >= 0) {
                            destroyBlock = false;
                        } else if (Arrays.binarySearch(fragileBlocks, b.getRelative(BlockFace.WEST).getTypeId()) >= 0) {
                            destroyBlock = false;
                        } else if (Arrays.binarySearch(fragileBlocks, b.getRelative(BlockFace.NORTH).getTypeId()) >= 0) {
                            destroyBlock = false;
                        }
                    }
                    if (!destroyBlock) {
                        i.remove();
                    }
                    // whether or not you actually destroyed the block, add to damages
                    long damages = assault.getDamages() + Settings.AssaultDamagesPerBlock;
                    if (damages < assault.getMaxDamages())
                        assault.setDamages(damages);
                    long curTime = System.currentTimeMillis();
                    // notify nearby players of the damages, do this 1 second later so all damages from this volley will be included
                    if (curTime >= lastDamagesUpdate + 4000) {
                        final Location floc = b.getLocation();
                        final World fworld = b.getWorld();
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                long fdamages = assault.getDamages();
                                for (Player p : fworld.getPlayers()) {
                                    if (Math.round(p.getLocation().getBlockX() / 1000.0) == Math.round(floc.getBlockX() / 1000.0))
                                        if (Math.round(p.getLocation().getBlockZ() / 1000.0) == Math.round(floc.getBlockZ() / 1000.0)) {
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

        if (e.getEntity() == null)
            return;
        for (Player p : e.getEntity().getWorld().getPlayers()) {
            org.bukkit.entity.Entity tnt = e.getEntity();

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
                    BukkitTask placeCobweb = new BukkitRunnable() {
                        @Override
                        public void run() {
                            fp.sendBlockChange(loc, 89, (byte) 0);
                        }
                    }.runTaskLater(Movecraft.getInstance(), 5);
                    // then remove it
                    BukkitTask removeCobweb = new BukkitRunnable() {
                        @Override
                        public void run() {
                            fp.sendBlockChange(loc, 0, (byte) 0);
                        }
                    }.runTaskLater(Movecraft.getInstance(), 160);
                }
            }
        }
    }
}
