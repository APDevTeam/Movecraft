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

import java.util.Arrays;
import java.util.HashSet;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.MathUtils;
import net.countercraft.movecraft.utils.MovecraftLocation;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class PlayerListener implements Listener {
	
	private static boolean waitingForReleaseConfirmation = false;
	
	public String checkCraftBorders(Craft craft) {
		HashSet<MovecraftLocation> craftBlocks=new HashSet<MovecraftLocation>(Arrays.asList(craft.getBlockList()));
		String ret=null;
		for(MovecraftLocation block : craftBlocks) {
			int x,y,z;
			x=block.getX()+1;
			y=block.getY()+0;
			z=block.getZ()+0;
			MovecraftLocation test=new MovecraftLocation(x,y,z);
			if(!craftBlocks.contains(test))
				if((Arrays.binarySearch(craft.getType().getAllowedBlocks(), craft.getW().getBlockTypeIdAt(x, y, z))>=0)||(Arrays.binarySearch(craft.getType().getAllowedBlocks(), (craft.getW().getBlockTypeIdAt(x, y, z)<<4)+craft.getW().getBlockAt(x, y, z).getData()+10000)>=0)) {
					ret="@ "+x+","+y+","+z;
				}
			x=block.getX()-1;
			y=block.getY()+0;
			z=block.getZ()+0;
			test=new MovecraftLocation(x,y,z);
			if(!craftBlocks.contains(test))
				if((Arrays.binarySearch(craft.getType().getAllowedBlocks(), craft.getW().getBlockTypeIdAt(x, y, z))>=0)||(Arrays.binarySearch(craft.getType().getAllowedBlocks(), (craft.getW().getBlockTypeIdAt(x, y, z)<<4)+craft.getW().getBlockAt(x, y, z).getData()+10000)>=0)) {
					ret="@ "+x+","+y+","+z;
				}
			x=block.getX()+0;
			y=block.getY()+1;
			z=block.getZ()+0;
			test=new MovecraftLocation(x,y,z);
			if(!craftBlocks.contains(test))
				if((Arrays.binarySearch(craft.getType().getAllowedBlocks(), craft.getW().getBlockTypeIdAt(x, y, z))>=0)||(Arrays.binarySearch(craft.getType().getAllowedBlocks(), (craft.getW().getBlockTypeIdAt(x, y, z)<<4)+craft.getW().getBlockAt(x, y, z).getData()+10000)>=0)) {
					ret="@ "+x+","+y+","+z;
				}
			x=block.getX()+0;
			y=block.getY()-1;
			z=block.getZ()+0;
			test=new MovecraftLocation(x,y,z);
			if(!craftBlocks.contains(test))
				if((Arrays.binarySearch(craft.getType().getAllowedBlocks(), craft.getW().getBlockTypeIdAt(x, y, z))>=0)||(Arrays.binarySearch(craft.getType().getAllowedBlocks(), (craft.getW().getBlockTypeIdAt(x, y, z)<<4)+craft.getW().getBlockAt(x, y, z).getData()+10000)>=0)) {
					ret="@ "+x+","+y+","+z;
				}
			x=block.getX()+0;
			y=block.getY()+0;
			z=block.getZ()+1;
			test=new MovecraftLocation(x,y,z);
			if(!craftBlocks.contains(test))
				if((Arrays.binarySearch(craft.getType().getAllowedBlocks(), craft.getW().getBlockTypeIdAt(x, y, z))>=0)||(Arrays.binarySearch(craft.getType().getAllowedBlocks(), (craft.getW().getBlockTypeIdAt(x, y, z)<<4)+craft.getW().getBlockAt(x, y, z).getData()+10000)>=0)) {
					ret="@ "+x+","+y+","+z;
				}
			x=block.getX()+0;
			y=block.getY()+0;
			z=block.getZ()+1;
			test=new MovecraftLocation(x,y,z);
			if(!craftBlocks.contains(test))
				if((Arrays.binarySearch(craft.getType().getAllowedBlocks(), craft.getW().getBlockTypeIdAt(x, y, z))>=0)||(Arrays.binarySearch(craft.getType().getAllowedBlocks(), (craft.getW().getBlockTypeIdAt(x, y, z)<<4)+craft.getW().getBlockAt(x, y, z).getData()+10000)>=0)) {
					ret="@ "+x+","+y+","+z;
				}
			x=block.getX()+1;
			y=block.getY()+1;
			z=block.getZ()+0;
			test=new MovecraftLocation(x,y,z);
			if(!craftBlocks.contains(test))
				if((Arrays.binarySearch(craft.getType().getAllowedBlocks(), craft.getW().getBlockTypeIdAt(x, y, z))>=0)||(Arrays.binarySearch(craft.getType().getAllowedBlocks(), (craft.getW().getBlockTypeIdAt(x, y, z)<<4)+craft.getW().getBlockAt(x, y, z).getData()+10000)>=0)) {
					ret="@ "+x+","+y+","+z;
				}
			x=block.getX()+1;
			y=block.getY()-1;
			z=block.getZ()+0;
			test=new MovecraftLocation(x,y,z);
			if(!craftBlocks.contains(test))
				if((Arrays.binarySearch(craft.getType().getAllowedBlocks(), craft.getW().getBlockTypeIdAt(x, y, z))>=0)||(Arrays.binarySearch(craft.getType().getAllowedBlocks(), (craft.getW().getBlockTypeIdAt(x, y, z)<<4)+craft.getW().getBlockAt(x, y, z).getData()+10000)>=0)) {
					ret="@ "+x+","+y+","+z;
				}
			x=block.getX()-1;
			y=block.getY()+1;
			z=block.getZ()+0;
			test=new MovecraftLocation(x,y,z);
			if(!craftBlocks.contains(test))
				if((Arrays.binarySearch(craft.getType().getAllowedBlocks(), craft.getW().getBlockTypeIdAt(x, y, z))>=0)||(Arrays.binarySearch(craft.getType().getAllowedBlocks(), (craft.getW().getBlockTypeIdAt(x, y, z)<<4)+craft.getW().getBlockAt(x, y, z).getData()+10000)>=0)) {
					ret="@ "+x+","+y+","+z;
				}
			x=block.getX()-1;
			y=block.getY()-1;
			z=block.getZ()+0;
			test=new MovecraftLocation(x,y,z);
			if(!craftBlocks.contains(test))
				if((Arrays.binarySearch(craft.getType().getAllowedBlocks(), craft.getW().getBlockTypeIdAt(x, y, z))>=0)||(Arrays.binarySearch(craft.getType().getAllowedBlocks(), (craft.getW().getBlockTypeIdAt(x, y, z)<<4)+craft.getW().getBlockAt(x, y, z).getData()+10000)>=0)) {
					ret="@ "+x+","+y+","+z;
				}
			x=block.getX()+0;
			y=block.getY()+1;
			z=block.getZ()+1;
			test=new MovecraftLocation(x,y,z);
			if(!craftBlocks.contains(test))
				if((Arrays.binarySearch(craft.getType().getAllowedBlocks(), craft.getW().getBlockTypeIdAt(x, y, z))>=0)||(Arrays.binarySearch(craft.getType().getAllowedBlocks(), (craft.getW().getBlockTypeIdAt(x, y, z)<<4)+craft.getW().getBlockAt(x, y, z).getData()+10000)>=0)) {
					ret="@ "+x+","+y+","+z;
				}
			x=block.getX()+0;
			y=block.getY()-1;
			z=block.getZ()+1;
			test=new MovecraftLocation(x,y,z);
			if(!craftBlocks.contains(test))
				if((Arrays.binarySearch(craft.getType().getAllowedBlocks(), craft.getW().getBlockTypeIdAt(x, y, z))>=0)||(Arrays.binarySearch(craft.getType().getAllowedBlocks(), (craft.getW().getBlockTypeIdAt(x, y, z)<<4)+craft.getW().getBlockAt(x, y, z).getData()+10000)>=0)) {
					ret="@ "+x+","+y+","+z;
				}
			x=block.getX()+0;
			y=block.getY()+1;
			z=block.getZ()-1;
			test=new MovecraftLocation(x,y,z);
			if(!craftBlocks.contains(test))
				if((Arrays.binarySearch(craft.getType().getAllowedBlocks(), craft.getW().getBlockTypeIdAt(x, y, z))>=0)||(Arrays.binarySearch(craft.getType().getAllowedBlocks(), (craft.getW().getBlockTypeIdAt(x, y, z)<<4)+craft.getW().getBlockAt(x, y, z).getData()+10000)>=0)) {
					ret="@ "+x+","+y+","+z;
				}
			x=block.getX()+0;
			y=block.getY()-1;
			z=block.getZ()-1;
			test=new MovecraftLocation(x,y,z);
			if(!craftBlocks.contains(test))
				if((Arrays.binarySearch(craft.getType().getAllowedBlocks(), craft.getW().getBlockTypeIdAt(x, y, z))>=0)||(Arrays.binarySearch(craft.getType().getAllowedBlocks(), (craft.getW().getBlockTypeIdAt(x, y, z)<<4)+craft.getW().getBlockAt(x, y, z).getData()+10000)>=0)) {
					ret="@ "+x+","+y+","+z;
				}
		}
		return ret;
	}

	@EventHandler
	public void onPLayerLogout( PlayerQuitEvent e ) {
		Craft c = CraftManager.getInstance().getCraftByPlayer( e.getPlayer() );

		if ( c != null ) {
			CraftManager.getInstance().removeCraft( c );
		}
	}

/*	public void onPlayerDamaged( EntityDamageByEntityEvent e ) {
		if ( e instanceof Player ) {
			Player p = ( Player ) e;
			CraftManager.getInstance().removeCraft( CraftManager.getInstance().getCraftByPlayer( p ) );
		}
	}*/
	
	@EventHandler
	public void onPlayerDeath( EntityDamageByEntityEvent e ) {  // changed to death so when you shoot up an airship and hit the pilot, it still sinks
		if ( e instanceof Player ) {
			Player p = ( Player ) e;
			CraftManager.getInstance().removeCraft( CraftManager.getInstance().getCraftByPlayer( p ) );
		}
	}

	@EventHandler
	public void onPlayerMove( PlayerMoveEvent event ) {
		final Craft c = CraftManager.getInstance().getCraftByPlayer( event.getPlayer() );
		if ( c != null ) {
			if ( c.isNotProcessing() && (!MathUtils.playerIsWithinBoundingPolygon( c.getHitBox(), c.getMinX(), c.getMinZ(), MathUtils.bukkit2MovecraftLoc( event.getPlayer().getLocation() ) )) ) {

				if ( !CraftManager.getInstance().getReleaseEvents().containsKey( event.getPlayer() ) && c.getType().getMoveEntities()) {
					if(Settings.ManOverBoardTimeout!=0)
						event.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "You have left your craft. You may return to your craft by typing /manoverboard any time before the timeout expires" ) ) );						
					else
						event.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "Release - Player has left craft" ) ) );
					if(c.getBlockList().length>11000) {
						event.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "Craft is too big to check its borders. Make sure this area is safe to release your craft in.")));												
					} else {
						String ret=checkCraftBorders(c);
						if(ret!=null) {
							event.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "WARNING! There are blocks near your craft that may merge with the craft "+ret)));						
							event.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "Reply: release to confirm the release of your ship")));						

							// Wait for user confirmation
							waitingForReleaseConfirmation = true;
							
							return;
						}
					}
					
					BukkitTask releaseTask = new BukkitRunnable() {

						@Override
						public void run() {
							CraftManager.getInstance().removeCraft( c );
						}

					}.runTaskLater( Movecraft.getInstance(), ( 20 * 30 ) );

					CraftManager.getInstance().getReleaseEvents().put( event.getPlayer(), releaseTask );
				}
			} else {
				if ( CraftManager.getInstance().getReleaseEvents().containsKey(event.getPlayer()) && c.getType().getMoveEntities()) {
					CraftManager.getInstance().removeReleaseTask(c);
				}
			}
		}
	}

/*	@EventHandler
	public void onPlayerHit( EntityDamageByEntityEvent event ) {
		if ( event.getEntity() instanceof Player && CraftManager.getInstance().getCraftByPlayer( ( Player ) event.getEntity() ) != null ) {
			CraftManager.getInstance().removeCraft( CraftManager.getInstance().getCraftByPlayer( ( Player ) event.getEntity() ) );
		}
	}   */
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
  		if (waitingForReleaseConfirmation) {
  			if (event.getMessage().equalsIgnoreCase("release")) {
  				final Craft c = CraftManager.getInstance().getCraftByPlayer( event.getPlayer() );
  				waitingForReleaseConfirmation = false;
  				
  				BukkitTask releaseTask = new BukkitRunnable() {

						@Override
						public void run() {
							CraftManager.getInstance().removeCraft( c );
						}

					}.runTaskLater( Movecraft.getInstance(), ( 20 * 30 ) );

					CraftManager.getInstance().getReleaseEvents().put( event.getPlayer(), releaseTask );
  			}
  		}
	}
}
