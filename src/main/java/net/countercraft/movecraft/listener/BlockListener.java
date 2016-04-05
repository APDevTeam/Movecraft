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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.CraftType;
import net.countercraft.movecraft.items.StorageChestItem;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.MapUpdateManager;
import net.countercraft.movecraft.utils.MathUtils;
import net.countercraft.movecraft.utils.MovecraftLocation;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Attachable;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;

public class BlockListener implements Listener {

	@EventHandler
	public void onBlockPlace( final BlockPlaceEvent e ) {
		if ( Settings.DisableCrates==true )
			return;
		if ( e.getBlockAgainst().getTypeId() == 33 && e.getBlockAgainst().getData() == ( ( byte ) 6 ) ) {
			e.setCancelled( true );
		} else if ( e.getItemInHand().getItemMeta() != null && e.getItemInHand().getItemMeta().getDisplayName() != null && e.getItemInHand().getItemMeta().getDisplayName().equalsIgnoreCase( String.format( I18nSupport.getInternationalisedString( "Item - Storage Crate name" ) ) ) ) {
			e.getBlockPlaced().setTypeId( 33 );
			Location l = e.getBlockPlaced().getLocation();
			MovecraftLocation l1 = new MovecraftLocation( l.getBlockX(), l.getBlockY(), l.getBlockZ() );
			StorageChestItem.createNewInventory( l1, e.getBlockPlaced().getWorld() );
			new BukkitRunnable() {

				@Override
				public void run() {
					e.getBlockPlaced().setData( ( byte ) 6 );
				}

			}.runTask( Movecraft.getInstance() );
		}
	}

	@EventHandler
	public void onPlayerInteract( PlayerInteractEvent event ) {

		if ( event.getAction() == Action.RIGHT_CLICK_BLOCK ) {
			if ( event.getClickedBlock().getTypeId() == 33 && event.getClickedBlock().getData() == ( ( byte ) 6 ) ) {
				if(Settings.DisableCrates==true)
					return;
				Location l = event.getClickedBlock().getLocation();
				MovecraftLocation l1 = new MovecraftLocation( l.getBlockX(), l.getBlockY(), l.getBlockZ() );
				Inventory i = StorageChestItem.getInventoryOfCrateAtLocation( l1, event.getPlayer().getWorld() );

				if ( i != null ) {
					event.getPlayer().openInventory( i );
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockBreak( final BlockBreakEvent e ) {

		if ( e.isCancelled() ) {
			return;
		}
		if(Settings.ProtectPilotedCrafts) {
			MovecraftLocation mloc=MathUtils.bukkit2MovecraftLoc(e.getBlock().getLocation());
			boolean blockInCraft=false;
			if(CraftManager.getInstance().getCraftsInWorld(e.getBlock().getWorld())!=null)
				for(Craft craft : CraftManager.getInstance().getCraftsInWorld(e.getBlock().getWorld())) {
					if(craft!=null) {
						for(MovecraftLocation tloc : craft.getBlockList()) {
							if(tloc.getX()==mloc.getX() && tloc.getY()==mloc.getY() && tloc.getZ()==mloc.getZ())
								blockInCraft=true;
						}
					}				
				}
			if(blockInCraft) {
				e.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "BLOCK IS PART OF A PILOTED CRAFT" ) ) );
				e.setCancelled(true);
			}
		}
		if ( e.getBlock().getTypeId() == 33 && e.getBlock().getData() == ( ( byte ) 6 ) ) {
			if(Settings.DisableCrates==true)
				return;
			Location l = e.getBlock().getLocation();
			MovecraftLocation l1 = new MovecraftLocation( l.getBlockX(), l.getBlockY(), l.getBlockZ() );
			for ( ItemStack i : StorageChestItem.getInventoryOfCrateAtLocation( l1, e.getBlock().getWorld() ).getContents() ) {
				if ( i != null ) {
					e.getBlock().getWorld().dropItemNaturally( e.getBlock().getLocation(), i );
				}
			}
			StorageChestItem.removeInventoryAtLocation( e.getBlock().getWorld(), l1 );
			e.setCancelled( true );
			e.getBlock().setType( Material.AIR );
			e.getBlock().getLocation().getWorld().dropItemNaturally( e.getBlock().getLocation(), new StorageChestItem().getItemStack() );
		}
	}

	// prevent items from dropping from moving crafts
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onItemSpawn( final ItemSpawnEvent e ) {
		if ( e.isCancelled() ) {
			return;
		}
		if(CraftManager.getInstance().getCraftsInWorld(e.getLocation().getWorld())!=null) {
			for(Craft tcraft : CraftManager.getInstance().getCraftsInWorld(e.getLocation().getWorld())) {
				if ( (!tcraft.isNotProcessing()) && MathUtils.playerIsWithinBoundingPolygon( tcraft.getHitBox(), tcraft.getMinX(), tcraft.getMinZ(), MathUtils.bukkit2MovecraftLoc(e.getLocation() ) ) ) {
					e.setCancelled(true);
					return;
				}
			}
		}
	}
	
	// prevent water from spreading on moving crafts
	@EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockFromTo(BlockFromToEvent e) {
		if ( e.isCancelled() ) {
			return;
		}
		Block block = e.getToBlock();
        if (block.getType() == Material.WATER) {
            if(CraftManager.getInstance().getCraftsInWorld(block.getWorld())!=null) {
    			for(Craft tcraft : CraftManager.getInstance().getCraftsInWorld(block.getWorld())) {
    				if ( (!tcraft.isNotProcessing()) && MathUtils.playerIsWithinBoundingPolygon( tcraft.getHitBox(), tcraft.getMinX(), tcraft.getMinZ(), MathUtils.bukkit2MovecraftLoc(block.getLocation() ) ) ) {
    					e.setCancelled(true);
    					return;
    				}
    			}
    		}
        }
	}

	// prevent fragile items from dropping on moving crafts
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPhysics(BlockPhysicsEvent event) {
		if ( event.isCancelled() ) {
			return;
		}
		Block block = event.getBlock();
		final int[] fragileBlocks = new int[]{ 26, 34, 50, 55, 63, 64, 65, 68, 69, 70, 71, 72, 75, 76, 77, 93, 94, 96, 131, 132, 143, 147, 148, 149, 150, 151, 171, 193, 194, 195, 196, 197 };
		if(CraftManager.getInstance().getCraftsInWorld(block.getWorld())!=null) {
			for(Craft tcraft : CraftManager.getInstance().getCraftsInWorld(block.getWorld())) {
				if ( (!tcraft.isNotProcessing()) && MathUtils.playerIsWithinBoundingPolygon( tcraft.getHitBox(), tcraft.getMinX(), tcraft.getMinZ(), MathUtils.bukkit2MovecraftLoc(block.getLocation() ) ) ) {
					boolean isFragile=(Arrays.binarySearch(fragileBlocks,block.getTypeId())>=0);
					if (isFragile) {
//						BlockFace face = ((Attachable) block).getAttachedFace();
//					    if (!event.getBlock().getRelative(face).getType().isSolid()) {
						    event.setCancelled(true);
						    return;
//					    }
					}
				}
			}
        }
	}

	private CraftType getCraftTypeFromString( String s ) {
		for ( CraftType t : CraftManager.getInstance().getCraftTypes() ) {
			if ( s.equalsIgnoreCase( t.getCraftName() ) ) {
				return t;
			}
		}

		return null;
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onSignChange(SignChangeEvent event) {
        Player p = event.getPlayer();
        if(p==null)
        	return;
        String signText=org.bukkit.ChatColor.stripColor(event.getLine(0));
        // did the player try to create a craft command sign?
        if(getCraftTypeFromString( signText ) != null) {
        	if(Settings.RequireCreatePerm==false) {
        		return;
        	}
			if(p.hasPermission( "movecraft." + org.bukkit.ChatColor.stripColor(event.getLine(0)) + ".create")==false) {
				p.sendMessage( String.format( I18nSupport.getInternationalisedString( "Insufficient Permissions" ) ) );
				event.setCancelled(true);
			}
        }
        if(signText.equalsIgnoreCase( "Cruise: OFF") || signText.equalsIgnoreCase( "Cruise: ON")) {
        	if(p.hasPermission( "movecraft.cruisesign")==false && Settings.RequireCreatePerm) {
				p.sendMessage( String.format( I18nSupport.getInternationalisedString( "Insufficient Permissions" ) ) );
				event.setCancelled(true);
			}
        }
        if(signText.equalsIgnoreCase( "Crew:")) {
            String crewName=org.bukkit.ChatColor.stripColor(event.getLine(1));
        	if(!p.getName().equalsIgnoreCase(crewName)) {
				p.sendMessage( String.format( I18nSupport.getInternationalisedString( "You can only create a Crew: sign for yourself" ) ) );
				event.setLine(1, p.getName());
//				event.setCancelled(true);
			}
        }
    }
	
	@EventHandler(priority = EventPriority.LOW)
	public void onBlockIgnite(BlockIgniteEvent event) {
		if(!Settings.FireballPenetration)
			return;
		if(event.isCancelled())
			return;
		// replace blocks with fire occasionally, to prevent fast craft from simply ignoring fire
		if(event.getCause()==BlockIgniteEvent.IgniteCause.FIREBALL) {
			Block testBlock=event.getBlock().getRelative(-1, 0, 0);
			if(!testBlock.getType().isBurnable())
				testBlock=event.getBlock().getRelative(1, 0, 0);
			if(!testBlock.getType().isBurnable())
				testBlock=event.getBlock().getRelative(0, 0, -1);
			if(!testBlock.getType().isBurnable())
				testBlock=event.getBlock().getRelative(0, 0, 1);
			
			if(testBlock.getType().isBurnable()) {
				boolean isBurnAllowed=true;
				// check to see if fire spread is allowed, don't check if worldguard integration is not enabled
				if(Movecraft.getInstance().getWorldGuardPlugin()!=null && (Settings.WorldGuardBlockMoveOnBuildPerm || Settings.WorldGuardBlockSinkOnPVPPerm)) {
					ApplicableRegionSet set = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(testBlock.getWorld()).getApplicableRegions(testBlock.getLocation());
					if(set.allows(DefaultFlag.FIRE_SPREAD)==false) {
						isBurnAllowed=false;
					}
				}
				if(isBurnAllowed)
					testBlock.setType(org.bukkit.Material.AIR);
			}
		}
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
	
	@EventHandler(priority=EventPriority.NORMAL)
    public void explodeEvent(EntityExplodeEvent e) {
		// Remove any blocks from the list that were adjacent to water, to prevent spillage
		Iterator<Block> i=e.blockList().iterator();
		if(Settings.DisableSpillProtection==false)
			while(i.hasNext()) {
				Block b=i.next();
				boolean isNearWater=false;
				if(b.getY()>b.getWorld().getSeaLevel())
					for(int mx=-1;mx<=1;mx++)
						for(int mz=-1;mz<=1;mz++)
							for(int my=0;my<=1;my++) {
								if(b.getRelative(mx,my,mz).getType()==Material.STATIONARY_WATER || b.getRelative(mx,my,mz).getType()==Material.WATER)
									isNearWater=true;
							}
				if(isNearWater) {
					i.remove();
				}
			}
			
		if(e.getEntity()==null)
			return;
		for (Player p : e.getEntity().getWorld().getPlayers()) {
			org.bukkit.entity.Entity tnt=e.getEntity();
			
	        if(e.getEntityType() == EntityType.PRIMED_TNT && Settings.TracerRateTicks!=0) {
				long minDistSquared=60*60;
				long maxDistSquared=Bukkit.getServer().getViewDistance()*16;
				maxDistSquared=maxDistSquared-16;
				maxDistSquared=maxDistSquared*maxDistSquared;
				// is the TNT within the view distance (rendered world) of the player, yet further than 60 blocks?
				if(p.getLocation().distanceSquared(tnt.getLocation())<maxDistSquared && p.getLocation().distanceSquared(tnt.getLocation())>=minDistSquared) {  // we use squared because its faster
					final Location loc=tnt.getLocation();
					final Player fp=p;
					final World fw=e.getEntity().getWorld();
					// then make a glowstone to look like the explosion, place it a little later so it isn't right in the middle of the volley
					BukkitTask placeCobweb = new BukkitRunnable() {
						@Override
						public void run() {
							fp.sendBlockChange(loc, 89, (byte) 0);
						}
					}.runTaskLater( Movecraft.getInstance(), 5 );
					// then remove it
					BukkitTask removeCobweb = new BukkitRunnable() {
						@Override
						public void run() {
							fp.sendBlockChange(loc, 0, (byte) 0);
						}
					}.runTaskLater( Movecraft.getInstance(), 160 );
				}
			}            
        }
    }
}
