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

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.CraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.MapUpdateCommand;
import net.countercraft.movecraft.utils.MapUpdateManager;
import net.countercraft.movecraft.utils.MathUtils;
import net.countercraft.movecraft.utils.MovecraftLocation;
import net.countercraft.movecraft.utils.Rotation;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.v1_8_R1.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.SignBlock;
import com.sk89q.worldedit.schematic.SchematicFormat;
import com.sk89q.worldedit.world.DataException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class InteractListener implements Listener {
	private static final Map<Player, Long> timeMap = new HashMap<Player, Long>();
	private static final Map<Player, Long> repairRightClickTimeMap = new HashMap<Player, Long>();

	@EventHandler
	public void onPlayerInteract( PlayerInteractEvent event ) {
		
		if ( event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK) {
			Material m = event.getClickedBlock().getType();
			if ( m.equals( Material.SIGN_POST ) || m.equals( Material.WALL_SIGN ) ) {
				Sign sign = ( Sign ) event.getClickedBlock().getState();
				String signText = org.bukkit.ChatColor.stripColor(sign.getLine( 0 ));

				if ( signText == null ) {
					return;
				}
				
				if ( org.bukkit.ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Remote Sign")) {
					MovecraftLocation sourceLocation=MathUtils.bukkit2MovecraftLoc( event.getClickedBlock().getLocation() );
					Craft foundCraft=null;
					if(CraftManager.getInstance().getCraftsInWorld(event.getClickedBlock().getWorld())!=null)
						for(Craft tcraft : CraftManager.getInstance().getCraftsInWorld(event.getClickedBlock().getWorld())) {
							if ( MathUtils.playerIsWithinBoundingPolygon( tcraft.getHitBox(), tcraft.getMinX(), tcraft.getMinZ(), sourceLocation ) ) {
								// don't use a craft with a null player. This is mostly to avoid trying to use subcrafts
								if(CraftManager.getInstance().getPlayerFromCraft(tcraft)!=null)
									foundCraft=tcraft;
							}
						}
					
					if(foundCraft==null) {
						event.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "ERROR: Remote Sign must be a part of a piloted craft!" ) ) );
						return;
					}
					
					if(foundCraft.getType().allowRemoteSign()==false) {
						event.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "ERROR: Remote Signs not allowed on this craft!" ) ) );
						return;						
					}

					String targetText=org.bukkit.ChatColor.stripColor(sign.getLine(1));
					MovecraftLocation foundLoc=null;
					for(MovecraftLocation tloc : foundCraft.getBlockList()) {
						Block tb=event.getClickedBlock().getWorld().getBlockAt(tloc.getX(), tloc.getY(), tloc.getZ());
						if(tb.getType().equals(Material.SIGN_POST) || tb.getType().equals(Material.WALL_SIGN)) {
							Sign ts=( Sign ) tb.getState();
							if(org.bukkit.ChatColor.stripColor(ts.getLine(0))!=null) 
								if(org.bukkit.ChatColor.stripColor(ts.getLine(0)).equalsIgnoreCase(targetText))
									foundLoc=tloc;
							if(org.bukkit.ChatColor.stripColor(ts.getLine(1))!=null) 
								if(org.bukkit.ChatColor.stripColor(ts.getLine(1)).equalsIgnoreCase(targetText)) {
									boolean isRemoteSign=false;
									if(org.bukkit.ChatColor.stripColor(ts.getLine(0))!=null)
										if(org.bukkit.ChatColor.stripColor(ts.getLine(0)).equalsIgnoreCase("Remote Sign"))
											isRemoteSign=true;
									if(!isRemoteSign)
										foundLoc=tloc;
								}
							if(org.bukkit.ChatColor.stripColor(ts.getLine(2))!=null) 
								if(org.bukkit.ChatColor.stripColor(ts.getLine(2)).equalsIgnoreCase(targetText))
									foundLoc=tloc;
							if(org.bukkit.ChatColor.stripColor(ts.getLine(3))!=null) 
								if(org.bukkit.ChatColor.stripColor(ts.getLine(3)).equalsIgnoreCase(targetText))
									foundLoc=tloc;
						}
					}
					
					if(foundLoc==null) {
						event.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "ERROR: Could not find target sign!" ) ) );
						return;						
					}

					Block newBlock=event.getClickedBlock().getWorld().getBlockAt(foundLoc.getX(), foundLoc.getY(), foundLoc.getZ());
					PlayerInteractEvent newEvent=new PlayerInteractEvent(event.getPlayer(), event.getAction(), event.getItem(), newBlock, event.getBlockFace());
					onPlayerInteract(newEvent);
					event.setCancelled( true );
					return;
				}
			}
		}

		if ( event.getAction() == Action.RIGHT_CLICK_BLOCK ) {
			Material m = event.getClickedBlock().getType();
			if ( m.equals( Material.SIGN_POST ) || m.equals( Material.WALL_SIGN ) ) {
				onSignRightClick( event );
			}
		} else if ( event.getAction() == Action.LEFT_CLICK_BLOCK ) {
			Material m = event.getClickedBlock().getType();
			if ( m.equals( Material.SIGN_POST ) || m.equals( Material.WALL_SIGN ) ) {
				if ( event.getClickedBlock() == null ) {
					return;
				}
				Sign sign = ( Sign ) event.getClickedBlock().getState();
				String signText = org.bukkit.ChatColor.stripColor(sign.getLine( 0 ));

				if ( signText == null ) {
					return;
				}

				if ( org.bukkit.ChatColor.stripColor(sign.getLine( 0 )).equals( "\\  ||  /" ) && org.bukkit.ChatColor.stripColor(sign.getLine( 1 )).equals( "==      ==" ) && org.bukkit.ChatColor.stripColor(sign.getLine( 2 )).equals( "/  ||  \\" ) ) {
					Craft craft = CraftManager.getInstance().getCraftByPlayer( event.getPlayer() );
					if ( craft != null ) {
						if ( event.getPlayer().hasPermission( "movecraft." + craft.getType().getCraftName() + ".rotate" ) ) {

							Long time = timeMap.get( event.getPlayer() );
							if ( time != null ) {
								long ticksElapsed = ( System.currentTimeMillis() - time ) / 50;

								// if the craft should go slower underwater, make time pass more slowly there
								if(craft.getType().getHalfSpeedUnderwater() && craft.getMinY()<craft.getW().getSeaLevel())
									ticksElapsed=ticksElapsed>>1;

								if ( Math.abs( ticksElapsed ) < craft.getType().getTickCooldown() ) {
									event.setCancelled( true );
									return;
								}
							}

							if ( MathUtils.playerIsWithinBoundingPolygon( craft.getHitBox(), craft.getMinX(), craft.getMinZ(), MathUtils.bukkit2MovecraftLoc( event.getPlayer().getLocation() ) ) ) {

								CraftManager.getInstance().getCraftByPlayer( event.getPlayer() ).rotate( Rotation.ANTICLOCKWISE, MathUtils.bukkit2MovecraftLoc( sign.getLocation() ) );

								timeMap.put( event.getPlayer(), System.currentTimeMillis() );
								event.setCancelled( true );

							}

						} else {
							event.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "Insufficient Permissions" ) ) );
						}
					}
				}
				if ( org.bukkit.ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Subcraft Rotate")) {
					// rotate subcraft
					String craftTypeStr=org.bukkit.ChatColor.stripColor(sign.getLine( 1 ));
					if ( getCraftTypeFromString( craftTypeStr ) != null ) {
						if( org.bukkit.ChatColor.stripColor(sign.getLine(2)).equals("") && org.bukkit.ChatColor.stripColor(sign.getLine(3)).equals("") ) {
							sign.setLine(2, "_\\ /_");
							sign.setLine(3, "/ \\");
							sign.update();
						}
						
						if ( event.getPlayer().hasPermission( "movecraft." + craftTypeStr + ".pilot" ) && event.getPlayer().hasPermission( "movecraft." + craftTypeStr + ".rotate" )) {
							Long time = timeMap.get( event.getPlayer() );
							if ( time != null ) {
								long ticksElapsed = ( System.currentTimeMillis() - time ) / 50;
								if ( Math.abs( ticksElapsed ) < getCraftTypeFromString( craftTypeStr ).getTickCooldown() ) {
									event.setCancelled( true );
									return;
								}
							}
							final Location loc = event.getClickedBlock().getLocation();
							final Craft c = new Craft( getCraftTypeFromString( craftTypeStr ), loc.getWorld() );
							MovecraftLocation startPoint = new MovecraftLocation( loc.getBlockX(), loc.getBlockY(), loc.getBlockZ() );
							c.detect( null, event.getPlayer(), startPoint );
							BukkitTask releaseTask = new BukkitRunnable() {

								@Override
								public void run() {
									CraftManager.getInstance().removeCraft( c );
								}

							}.runTaskLater( Movecraft.getInstance(), ( 20 * 5 ) );

							BukkitTask rotateTask = new BukkitRunnable() {

								@Override
								public void run() {
									c.rotate( Rotation.ANTICLOCKWISE, MathUtils.bukkit2MovecraftLoc( loc ),true );
								}

							}.runTaskLater( Movecraft.getInstance(), ( 10 ) );
							timeMap.put( event.getPlayer(), System.currentTimeMillis() );
							event.setCancelled( true );
						} else {
							event.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "Insufficient Permissions" ) ) );							
						}
					}
				}
				if ( org.bukkit.ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Repair:")) { // left click the Repair sign, and it saves the state
					if( Settings.RepairTicksPerBlock==0) {
						event.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "Repair functionality is disabled or WorldEdit was not detected" ) ) );
						return;
					}
					Craft pCraft = CraftManager.getInstance().getCraftByPlayer( event.getPlayer() );
					if(pCraft==null) {
						event.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "You must be piloting a craft" ) ) );
						return;
					}
					
					String repairStateName=Movecraft.getInstance().getDataFolder().getAbsolutePath() + "/RepairStates";
					File file = new File(repairStateName);
					if( !file.exists() ) {
						file.mkdirs();
					}
					repairStateName+="/";
					repairStateName+=event.getPlayer().getName();
					repairStateName+=sign.getLine(1);
					file = new File(repairStateName);
					
					Vector size=new Vector(pCraft.getMaxX()-pCraft.getMinX(),(pCraft.getMaxY()-pCraft.getMinY())+1,pCraft.getMaxZ()-pCraft.getMinZ());
					Vector origin=new Vector(sign.getX(),sign.getY(),sign.getZ());
					Vector offset=new Vector(pCraft.getMinX()-sign.getX(),pCraft.getMinY()-sign.getY(),pCraft.getMinZ()-sign.getZ());
					CuboidClipboard cc = new CuboidClipboard(size,origin,offset);
					final int[] ignoredBlocks = new int[]{ 26,34,64,71,140,144,176,177,193,194,195,196,197 };  // BLOCKS THAT CAN'T BE PARTIALLY RECONSTRUCTED

					for(MovecraftLocation loc : pCraft.getBlockList()) {
						Vector ccpos = new Vector(loc.getX()-pCraft.getMinX(),loc.getY()-pCraft.getMinY(),loc.getZ()-pCraft.getMinZ());
						Block b=sign.getWorld().getBlockAt(loc.getX(), loc.getY(), loc.getZ());
						boolean isIgnored=(Arrays.binarySearch(ignoredBlocks,b.getTypeId())>=0);
						if(!isIgnored) {
							BaseBlock bb;
							BlockState state=b.getState();
							if(state instanceof Sign) {
								Sign s=(Sign)state;
								SignBlock sb=new SignBlock(b.getTypeId(), b.getData(), s.getLines());
								bb=(BaseBlock)sb;
							} else {
								bb=new BaseBlock(b.getTypeId(),b.getData());
							}
							cc.setBlock(ccpos, bb);
						}
					}
					try {
						cc.saveSchematic(file);
					} catch (IOException e) {
						event.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "Could not save file" ) ) );
						e.printStackTrace();
						return;
					} catch (DataException e) {
						event.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "Could not save file" ) ) );
						e.printStackTrace();
						return;
					}
					event.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "State saved" ) ) );
					event.setCancelled(true);
				}
			}
		}
	}
	
	private void onSignRightClick( PlayerInteractEvent event ) {
		Sign sign = ( Sign ) event.getClickedBlock().getState();
		String signText = org.bukkit.ChatColor.stripColor(sign.getLine( 0 ));

		if ( signText == null ) {
			return;
		}
		
		// don't process commands if this is a pilot tool click, do that below
		if ( event.getItem() != null && event.getItem().getTypeId()==Settings.PilotTool ) {
			Craft c = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
			if(c!=null)
				return;
		}


		if ( getCraftTypeFromString( org.bukkit.ChatColor.stripColor(sign.getLine( 0 )) ) != null ) {

			// Valid sign prompt for ship command.
			if ( event.getPlayer().hasPermission( "movecraft." + org.bukkit.ChatColor.stripColor(sign.getLine( 0 )) + ".pilot" ) ) {
				// Attempt to run detection
				Location loc = event.getClickedBlock().getLocation();
				MovecraftLocation startPoint = new MovecraftLocation( loc.getBlockX(), loc.getBlockY(), loc.getBlockZ() );
				final Craft c = new Craft( getCraftTypeFromString( org.bukkit.ChatColor.stripColor(sign.getLine( 0 )) ), loc.getWorld() );
				
				if(c.getType().getCruiseOnPilot()==true) {
					c.detect( null, event.getPlayer(), startPoint );
					c.setCruiseDirection(sign.getRawData());
					c.setLastCruisUpdate(System.currentTimeMillis());
					c.setCruising(true);
					BukkitTask releaseTask = new BukkitRunnable() {

						@Override
						public void run() {
							CraftManager.getInstance().removeCraft( c );
						}

					}.runTaskLater( Movecraft.getInstance(), ( 20 * 15 ) );
//					CraftManager.getInstance().getReleaseEvents().put( event.getPlayer(), releaseTask );
				} else {
					if ( CraftManager.getInstance().getCraftByPlayer( event.getPlayer() ) == null ) {
						c.detect( event.getPlayer(), event.getPlayer(), startPoint );
					} else {
						Craft oldCraft=CraftManager.getInstance().getCraftByPlayer( event.getPlayer() );
						CraftManager.getInstance().removeCraft( oldCraft );
						c.detect( event.getPlayer(), event.getPlayer(), startPoint );
					}
				}
				
				event.setCancelled( true );
			} else {
			event.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "Insufficient Permissions" ) ) );

			}

		} else if ( org.bukkit.ChatColor.stripColor(sign.getLine( 0 )).equalsIgnoreCase( "[helm]" ) ) {
			sign.setLine( 0, "\\  ||  /" );
			sign.setLine( 1, "==      ==" );
			sign.setLine( 2, "/  ||  \\" );
			sign.update( true );
			event.setCancelled( true );
		} else if ( org.bukkit.ChatColor.stripColor(sign.getLine( 0 )).equals( "\\  ||  /" ) && org.bukkit.ChatColor.stripColor(sign.getLine( 1 )).equals( "==      ==" ) && org.bukkit.ChatColor.stripColor(sign.getLine( 2 )).equals( "/  ||  \\" ) ) {
			Craft craft = CraftManager.getInstance().getCraftByPlayer( event.getPlayer() );
			if ( craft != null ) {
				if ( event.getPlayer().hasPermission( "movecraft." + craft.getType().getCraftName() + ".rotate" ) ) {
					Long time = timeMap.get( event.getPlayer() );
					if ( time != null ) {
						long ticksElapsed = ( System.currentTimeMillis() - time ) / 50;

						// if the craft should go slower underwater, make time pass more slowly there
						if(craft.getType().getHalfSpeedUnderwater() && craft.getMinY()<craft.getW().getSeaLevel())
							ticksElapsed=ticksElapsed>>1;

						if ( Math.abs( ticksElapsed ) < craft.getType().getTickCooldown() ) {
							event.setCancelled( true );
							return;
						}
					}

					if ( MathUtils.playerIsWithinBoundingPolygon( craft.getHitBox(), craft.getMinX(), craft.getMinZ(), MathUtils.bukkit2MovecraftLoc( event.getPlayer().getLocation() ) ) ) {

						CraftManager.getInstance().getCraftByPlayer( event.getPlayer() ).rotate( Rotation.CLOCKWISE, MathUtils.bukkit2MovecraftLoc( sign.getLocation() ) );

						timeMap.put( event.getPlayer(), System.currentTimeMillis() );
						event.setCancelled( true );

					}

				} else {
					event.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "Insufficient Permissions" ) ) );
				}
			}
		} else if ( org.bukkit.ChatColor.stripColor(sign.getLine( 0 )).equalsIgnoreCase( "Subcraft Rotate")) {
			// rotate subcraft
			String craftTypeStr=org.bukkit.ChatColor.stripColor(sign.getLine( 1 ));
			if ( getCraftTypeFromString( craftTypeStr ) != null ) {
				if( org.bukkit.ChatColor.stripColor(sign.getLine(2)).equals("") && sign.getLine(3).equals("") ) {
					sign.setLine(2, "_\\ /_");
					sign.setLine(3, "/ \\");
					sign.update();
				}
				
				if ( event.getPlayer().hasPermission( "movecraft." + craftTypeStr + ".pilot" ) && event.getPlayer().hasPermission( "movecraft." + craftTypeStr + ".rotate" )) {
					Long time = timeMap.get( event.getPlayer() );
					if ( time != null ) {
						long ticksElapsed = ( System.currentTimeMillis() - time ) / 50;
						if ( Math.abs( ticksElapsed ) < getCraftTypeFromString( craftTypeStr ).getTickCooldown() ) {
							event.setCancelled( true );
							return;
						}
					}
					final Location loc = event.getClickedBlock().getLocation();
					final Craft c = new Craft( getCraftTypeFromString( craftTypeStr ), loc.getWorld() );
					MovecraftLocation startPoint = new MovecraftLocation( loc.getBlockX(), loc.getBlockY(), loc.getBlockZ() );
					c.detect( null, event.getPlayer(), startPoint );
					BukkitTask releaseTask = new BukkitRunnable() {

						@Override
						public void run() {
							CraftManager.getInstance().removeCraft( c );
						}

					}.runTaskLater( Movecraft.getInstance(), ( 20 * 5 ) );

					BukkitTask rotateTask = new BukkitRunnable() {

						@Override
						public void run() {
							c.rotate( Rotation.CLOCKWISE, MathUtils.bukkit2MovecraftLoc( loc ),true );
						}

					}.runTaskLater( Movecraft.getInstance(), ( 10 ) );
					timeMap.put( event.getPlayer(), System.currentTimeMillis() );
					event.setCancelled( true );
				} else {
					event.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "Insufficient Permissions" ) ) );							
				}
			}		
		} else if ( org.bukkit.ChatColor.stripColor(sign.getLine( 0 )).equalsIgnoreCase( "Cruise: OFF")) {
			if(CraftManager.getInstance().getCraftByPlayer( event.getPlayer() )!=null){
				Craft c = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
				if(c.getType().getCanCruise()) {
					c.resetSigns(false, true, true);
					sign.setLine( 0, "Cruise: ON" );
					sign.update( true );

					CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).setCruiseDirection(sign.getRawData());
					CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).setLastCruisUpdate(System.currentTimeMillis());
					CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).setCruising(true);
					
					if (!c.getType().getMoveEntities()){
						 CraftManager.getInstance().addReleaseTask(c);
					}
				}
			}
		} else if ( org.bukkit.ChatColor.stripColor(sign.getLine( 0 )).equalsIgnoreCase( "Ascend: OFF")) {
			if(CraftManager.getInstance().getCraftByPlayer( event.getPlayer() )!=null){
				Craft c = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
				if(c.getType().getCanCruise()) {
					c.resetSigns(true, false, true);
					sign.setLine( 0, "Ascend: ON" );
					sign.update( true );

					CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).setCruiseDirection((byte) 0x42);
					CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).setLastCruisUpdate(System.currentTimeMillis());
					CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).setCruising(true);
					
					if (!c.getType().getMoveEntities()){
						 CraftManager.getInstance().addReleaseTask(c);
					}
				}
			}
		} else if ( org.bukkit.ChatColor.stripColor(sign.getLine( 0 )).equalsIgnoreCase( "Descend: OFF")) {
			if(CraftManager.getInstance().getCraftByPlayer( event.getPlayer() )!=null){
				Craft c = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
				if(c.getType().getCanCruise()) {
					c.resetSigns(true, true, false);
					sign.setLine( 0, "Descend: ON" );
					sign.update( true );

					CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).setCruiseDirection((byte) 0x43);
					CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).setLastCruisUpdate(System.currentTimeMillis());
					CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).setCruising(true);
					
					if (!c.getType().getMoveEntities()){
						 CraftManager.getInstance().addReleaseTask(c);
					}
				}
			}
		} else if ( org.bukkit.ChatColor.stripColor(sign.getLine( 0 )).equalsIgnoreCase( "Cruise: ON")) {
			if(CraftManager.getInstance().getCraftByPlayer( event.getPlayer() )!=null)
				if(CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).getType().getCanCruise()) {
					sign.setLine( 0, "Cruise: OFF" );
					sign.update( true );
					CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).setCruising(false);
				}
		} else if ( org.bukkit.ChatColor.stripColor(sign.getLine( 0 )).equalsIgnoreCase( "Ascend: ON")) {
			if(CraftManager.getInstance().getCraftByPlayer( event.getPlayer() )!=null)
				if(CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).getType().getCanCruise()) {
					sign.setLine( 0, "Ascend: OFF" );
					sign.update( true );
					CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).setCruising(false);
				}
		} else if ( org.bukkit.ChatColor.stripColor(sign.getLine( 0 )).equalsIgnoreCase( "Descend: ON")) {
			if(CraftManager.getInstance().getCraftByPlayer( event.getPlayer() )!=null)
				if(CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).getType().getCanCruise()) {
					sign.setLine( 0, "Descend: OFF" );
					sign.update( true );
					CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).setCruising(false);
				}
		} else if ( org.bukkit.ChatColor.stripColor(sign.getLine( 0 )).equalsIgnoreCase("Teleport:")) {
			if(CraftManager.getInstance().getCraftByPlayer( event.getPlayer() )!=null) {
				String[] numbers = org.bukkit.ChatColor.stripColor(sign.getLine( 1 )).split(",");
				int tX=Integer.parseInt(numbers[0]);
				int tY=Integer.parseInt(numbers[1]);
				int tZ=Integer.parseInt(numbers[2]);

				if(event.getPlayer().hasPermission( "movecraft." + CraftManager.getInstance().getCraftByPlayer( event.getPlayer()).getType().getCraftName() + ".move")) {					
					if(CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).getType().getCanTeleport()) {
						int dx=tX-sign.getX();
						int dy=tY-sign.getY();
						int dz=tZ-sign.getZ();
						CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).translate(dx, dy, dz);;
					}
				} else {
					event.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "Insufficient Permissions" ) ) );
				}
			}
		} else if ( org.bukkit.ChatColor.stripColor(sign.getLine( 0 )).equalsIgnoreCase( "Release")) {
			if(CraftManager.getInstance().getCraftByPlayer( event.getPlayer() )!=null) {
				Craft oldCraft=CraftManager.getInstance().getCraftByPlayer( event.getPlayer() );
				CraftManager.getInstance().removeCraft( oldCraft );
			}
		} else if ( org.bukkit.ChatColor.stripColor(sign.getLine( 0 )).equalsIgnoreCase("Move:")) {
			if(CraftManager.getInstance().getCraftByPlayer( event.getPlayer() )!=null) {
				Long time = timeMap.get( event.getPlayer() );
				if ( time != null ) {
					long ticksElapsed = ( System.currentTimeMillis() - time ) / 50;
					
					Craft craft=CraftManager.getInstance().getCraftByPlayer( event.getPlayer() );
					// if the craft should go slower underwater, make time pass more slowly there
					if(craft.getType().getHalfSpeedUnderwater() && craft.getMinY()<craft.getW().getSeaLevel())
						ticksElapsed=ticksElapsed>>1;

					if ( Math.abs( ticksElapsed ) < CraftManager.getInstance().getCraftByPlayer( event.getPlayer() ).getType().getTickCooldown() ) {
						event.setCancelled( true );
						return;
					}
				}
				String[] numbers = org.bukkit.ChatColor.stripColor(sign.getLine( 1 )).split(",");
				int dx=Integer.parseInt(numbers[0]);
				int dy=Integer.parseInt(numbers[1]);
				int dz=Integer.parseInt(numbers[2]);
				int maxMove=CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).getType().maxStaticMove();
				
				if(dx>maxMove)
					dx=maxMove;
				if(dx<0-maxMove)
					dx=0-maxMove;
				if(dy>maxMove)
					dy=maxMove;
				if(dy<0-maxMove)
					dy=0-maxMove;
				if(dz>maxMove)
					dz=maxMove;
				if(dz<0-maxMove)
					dz=0-maxMove;

				if(event.getPlayer().hasPermission( "movecraft." + CraftManager.getInstance().getCraftByPlayer( event.getPlayer()).getType().getCraftName() + ".move")) {
					if(CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).getType().getCanStaticMove()) {

						CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).translate(dx, dy, dz);
						timeMap.put( event.getPlayer(), System.currentTimeMillis() );
						CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).setLastCruisUpdate(System.currentTimeMillis());

					}
				} else {
					event.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "Insufficient Permissions" ) ) );
				}
			}
		} else  if ( org.bukkit.ChatColor.stripColor(sign.getLine( 0 )).equalsIgnoreCase("RMove:")) {
			if(CraftManager.getInstance().getCraftByPlayer( event.getPlayer() )!=null) {
				Long time = timeMap.get( event.getPlayer() );
				if ( time != null ) {
					long ticksElapsed = ( System.currentTimeMillis() - time ) / 50;

					Craft craft=CraftManager.getInstance().getCraftByPlayer( event.getPlayer() );
					// if the craft should go slower underwater, make time pass more slowly there
					if(craft.getType().getHalfSpeedUnderwater() && craft.getMinY()<craft.getW().getSeaLevel())
						ticksElapsed=ticksElapsed>>1;

					if ( Math.abs( ticksElapsed ) < CraftManager.getInstance().getCraftByPlayer( event.getPlayer() ).getType().getTickCooldown() ) {
						event.setCancelled( true );
						return;
					}
				}
				String[] numbers = org.bukkit.ChatColor.stripColor(sign.getLine( 1 )).split(",");
				int dLeftRight=Integer.parseInt(numbers[0]); // negative = left, positive = right
				int dy=Integer.parseInt(numbers[1]);
				int dBackwardForward=Integer.parseInt(numbers[2]); // negative = backwards, positive = forwards
				int maxMove=CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).getType().maxStaticMove();
				
				if(dLeftRight>maxMove) 
					dLeftRight=maxMove;
				if(dLeftRight<0-maxMove) 
					dLeftRight=0-maxMove;
				if(dy>maxMove)
					dy=maxMove;
				if(dy<0-maxMove)
					dy=0-maxMove;
				if(dBackwardForward>maxMove)
					dBackwardForward=maxMove;
				if(dBackwardForward<0-maxMove)
					dBackwardForward=0-maxMove;
				int dx=0;
				int dz=0;
				switch(sign.getRawData()) {
				case 0x3:
					// North
					dx=dLeftRight;
					dz=0-dBackwardForward;
					break;
				case 0x2:
					// South
					dx=0-dLeftRight;
					dz=dBackwardForward;
					break;
				case 0x4:
					// East
					dx=dBackwardForward;
					dz=dLeftRight;
					break;
				case 0x5:
					// West
					dx=0-dBackwardForward;
					dz=0-dLeftRight;
					break;
				}

				if(event.getPlayer().hasPermission( "movecraft." + CraftManager.getInstance().getCraftByPlayer( event.getPlayer()).getType().getCraftName() + ".move")) {
					if(CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).getType().getCanStaticMove()) {

						CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).translate(dx, dy, dz);
						timeMap.put( event.getPlayer(), System.currentTimeMillis() );
						CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).setLastCruisUpdate(System.currentTimeMillis());
						
					}
				} else {
					event.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "Insufficient Permissions" ) ) );
				}
			}
		} else if ( org.bukkit.ChatColor.stripColor(sign.getLine( 0 )).equalsIgnoreCase("Repair:")) {
			if( Settings.RepairTicksPerBlock==0) {
				event.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "Repair functionality is disabled or WorldEdit was not detected" ) ) );
				return;
			}
			Craft pCraft = CraftManager.getInstance().getCraftByPlayer( event.getPlayer() );
			if(pCraft==null) {
				event.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "You must be piloting a craft" ) ) );
				return;
			}
			if( !event.getPlayer().hasPermission( "movecraft." + pCraft.getType().getCraftName() + ".repair")) {
				event.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "Insufficient Permissions" ) ) );
				return;
			}
			// load up the repair state
			
			String repairStateName=Movecraft.getInstance().getDataFolder().getAbsolutePath() + "/RepairStates";
			repairStateName+="/";
			repairStateName+=event.getPlayer().getName();
			repairStateName+=sign.getLine(1);
			File file = new File(repairStateName);
			if( !file.exists() ) {
				event.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "REPAIR STATE NOT FOUND" ) ) );
				return;
			}
			SchematicFormat sf=SchematicFormat.getFormat(file);
			CuboidClipboard cc;
			try {
				cc = sf.load(file);
			} catch (com.sk89q.worldedit.data.DataException e) {
				event.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "REPAIR STATE NOT FOUND" ) ) );
				e.printStackTrace();
				return;				
			} catch (IOException e) {
				event.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "REPAIR STATE NOT FOUND" ) ) );
				e.printStackTrace();
				return;				
			}
			
			// calculate how many and where the blocks need to be replaced
			Location worldLoc=new Location(sign.getWorld(),sign.getX(),sign.getY(),sign.getZ());
			int numdiffblocks=0;
			HashMap<Integer,Integer> numMissingItems=new HashMap<Integer,Integer>(); // block type, number missing
			HashSet<Vector> locMissingBlocks=new HashSet<Vector>(); 
			for(int x=0;x<cc.getWidth();x++) {
				for(int y=0;y<cc.getHeight();y++) {
					for(int z=0;z<cc.getLength();z++) {
						Vector ccLoc=new Vector(x,y,z);
						worldLoc.setX(sign.getX()+cc.getOffset().getBlockX()+x);
						worldLoc.setY(sign.getY()+cc.getOffset().getBlockY()+y);
						worldLoc.setZ(sign.getZ()+cc.getOffset().getBlockZ()+z);
						Boolean isImportant=true;
						if(!pCraft.getType().blockedByWater())
							if(cc.getBlock(ccLoc).getId()==8 || cc.getBlock(ccLoc).getId()==9)
								isImportant=false;
						if(cc.getBlock(ccLoc).getId()==0)
							isImportant=false;
						if(isImportant && worldLoc.getWorld().getBlockAt(worldLoc).getTypeId()!=cc.getBlock(ccLoc).getId() ) {
							numdiffblocks++;
							int itemToConsume=cc.getBlock(ccLoc).getId();
							//some blocks aren't represented by items with the same number as the block
							if(itemToConsume==63 || itemToConsume==68) // signs
								itemToConsume=323;
							if(itemToConsume==93 || itemToConsume==94) // repeaters
								itemToConsume=356;
							if(itemToConsume==149 || itemToConsume==150) // comparators
								itemToConsume=404;
							if(itemToConsume==55) // redstone
								itemToConsume=331;
							if(itemToConsume==118) // cauldron
								itemToConsume=380;
							if(itemToConsume==124) // lit redstone lamp
								itemToConsume=123;
							if(itemToConsume==75) // lit redstone torch
								itemToConsume=76;
							if( !numMissingItems.containsKey(itemToConsume) ) {
								numMissingItems.put(itemToConsume, 1);
							} else {
								Integer num=numMissingItems.get(itemToConsume);
								num++;
								numMissingItems.put(itemToConsume, num);
							}
							locMissingBlocks.add(ccLoc);
						}
					}
				}
			}
			
			// if this is the second click in the last 5 seconds, start the repair, otherwise give them the info on the repair
			Boolean secondClick=false;
			Long time=repairRightClickTimeMap.get(event.getPlayer());
			if(time!=null) {
				long ticksElapsed = ( System.currentTimeMillis() - time ) / 50;
				if(ticksElapsed<100) {
					secondClick=true;
				}
			}
			if(secondClick) {
				// check all the chests for materials for the repair
				HashMap<Integer,ArrayList<InventoryHolder>> chestsToTakeFrom=new HashMap<Integer,ArrayList<InventoryHolder>>(); // typeid, list of chest inventories
				boolean enoughMaterial=true;
				for (Integer typeID : numMissingItems.keySet()) {
					int remainingQty=numMissingItems.get(typeID);
					ArrayList<InventoryHolder> chests=new ArrayList<InventoryHolder>();
					for (MovecraftLocation loc : pCraft.getBlockList()) {
	                    Block b=pCraft.getW().getBlockAt(loc.getX(), loc.getY(), loc.getZ());
	                    if(b.getTypeId()==54) {
	                        InventoryHolder inventoryHolder = ( InventoryHolder ) b.getState();
	                        if(inventoryHolder.getInventory().contains(typeID) && remainingQty>0) {
	                        	HashMap<Integer, ? extends ItemStack> foundItems=inventoryHolder.getInventory().all(typeID);
	                        	// count how many were in the chest
	                        	int numfound=0;
	                        	for(ItemStack istack : foundItems.values()) {
	                        		numfound+=istack.getAmount();
	                        	}
	                        	remainingQty-=numfound;
	                        	chests.add(inventoryHolder);
	                        }					
	                    }
	                }
	                if(remainingQty>0) {
						event.getPlayer().sendMessage(String.format( I18nSupport.getInternationalisedString( "Need more of material" )+": %s - %d",Material.getMaterial(typeID).name().toLowerCase().replace("_"," "),remainingQty));
						enoughMaterial=false;
	                } else {
	                	chestsToTakeFrom.put(typeID, chests);
	                }
				}
				if(Movecraft.getInstance().getEconomy()!=null && enoughMaterial) {
					double moneyCost=numdiffblocks*Settings.RepairMoneyPerBlock;
					if(Movecraft.getInstance().getEconomy().has(event.getPlayer(), moneyCost)) {
						Movecraft.getInstance().getEconomy().withdrawPlayer(event.getPlayer(), moneyCost);
					} else {
						event.getPlayer().sendMessage(String.format( I18nSupport.getInternationalisedString( "You do not have enough money" )));
						enoughMaterial=false;
					}
				}
				if(!enoughMaterial) {
					return;
				} else {
					// we know we have enough materials to make the repairs, so remove the materials from the chests
					for (Integer typeID : numMissingItems.keySet()) {
						int remainingQty=numMissingItems.get(typeID);
						for (InventoryHolder inventoryHolder : chestsToTakeFrom.get(typeID)) {
							HashMap<Integer, ? extends ItemStack> foundItems=inventoryHolder.getInventory().all(typeID);
                        	for(ItemStack istack : foundItems.values()) {
                        		if(istack.getAmount()<=remainingQty) {
                        			remainingQty-=istack.getAmount();
                            		inventoryHolder.getInventory().removeItem(istack);                        			
                        		} else {
                        			istack.setAmount(istack.getAmount()-remainingQty);
                        			remainingQty=0;
                        		}
                        	}

						}
					}
					ArrayList <MapUpdateCommand> updateCommands=new ArrayList <MapUpdateCommand>();
					for(Vector ccloc : locMissingBlocks) {
						BaseBlock bb=cc.getBlock(ccloc);
						if(bb.getId()==68 || bb.getId()==63) { // I don't know why this is necessary. I'm pretty sure WE should be loading signs as signblocks, but it doesn't seem to
							SignBlock sb=new SignBlock(bb.getId(), bb.getData());
							sb.setNbtData(bb.getNbtData());
							bb=sb;
						}
						MovecraftLocation moveloc=new MovecraftLocation(sign.getX()+cc.getOffset().getBlockX()+ccloc.getBlockX(),sign.getY()+cc.getOffset().getBlockY()+ccloc.getBlockY(),sign.getZ()+cc.getOffset().getBlockZ()+ccloc.getBlockZ());
						MapUpdateCommand updateCom=new MapUpdateCommand(moveloc,bb.getType(),bb,pCraft);
						updateCommands.add(updateCom);
					}
					if(updateCommands.size()>0) {
						final Craft fpCraft=pCraft;
						final MapUpdateCommand[] fUpdateCommands=updateCommands.toArray(new MapUpdateCommand[1]);
						int durationInTicks=numdiffblocks*Settings.RepairTicksPerBlock;
						
						// send out status updates every minute
						for(int ticsFromStart=0; ticsFromStart<durationInTicks; ticsFromStart+=1200) {
							final Player fp=event.getPlayer();
							final int fTics=ticsFromStart/20;
							final int fDur=durationInTicks/20;
							BukkitTask statusTask = new BukkitRunnable() {
								@Override
								public void run() {
									fp.sendMessage(String.format( I18nSupport.getInternationalisedString( "Repairs underway" )+": %d / %d",fTics,fDur));								
								}
							}.runTaskLater( Movecraft.getInstance(), (ticsFromStart) );
							
						}

						// keep craft piloted during the repair process so player can not move it
						CraftManager.getInstance().removePlayerFromCraft(pCraft);
						final Craft releaseCraft=pCraft;
						final Player fp=event.getPlayer();
						BukkitTask releaseTask = new BukkitRunnable() {
							@Override
							public void run() {
								CraftManager.getInstance().removeCraft(releaseCraft);
								fp.sendMessage(String.format( I18nSupport.getInternationalisedString( "Repairs complete. You may now pilot the craft" )));								
							}
						}.runTaskLater( Movecraft.getInstance(), (durationInTicks+20) );

						//do the actual repair
						BukkitTask repairTask = new BukkitRunnable() {
							@Override
							public void run() {
								MapUpdateManager.getInstance().addWorldUpdate( fpCraft.getW(), fUpdateCommands, null,null);
							}
						}.runTaskLater( Movecraft.getInstance(), (durationInTicks) );
						
					}
				}
				
			} else {
				// if this is the first time they have clicked the sign, show the summary of repair costs and requirements
				event.getPlayer().sendMessage(String.format( I18nSupport.getInternationalisedString( "Total damaged blocks" )+": %d", numdiffblocks));
				float percent=(numdiffblocks*100)/pCraft.getOrigBlockCount();
				event.getPlayer().sendMessage(String.format( I18nSupport.getInternationalisedString( "Percentage of craft" )+": %.2f%%",percent));
				if(percent>50) {
					event.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "This craft is too damaged and can not be repaired" ) ) );					
					return;
				}
				if(numdiffblocks!=0) {
					event.getPlayer().sendMessage(String.format( I18nSupport.getInternationalisedString( "SUPPLIES NEEDED" )));
					for(Integer blockTypeInteger : numMissingItems.keySet()) {
						event.getPlayer().sendMessage( String.format( "%s : %d",Material.getMaterial(blockTypeInteger).name().toLowerCase().replace("_"," "),numMissingItems.get(blockTypeInteger)));
					}
					int durationInSeconds=numdiffblocks*Settings.RepairTicksPerBlock/20;
					event.getPlayer().sendMessage(String.format( I18nSupport.getInternationalisedString( "Seconds to complete repair" )+": %d",durationInSeconds));
					int moneyCost=(int) (numdiffblocks*Settings.RepairMoneyPerBlock);
					event.getPlayer().sendMessage(String.format( I18nSupport.getInternationalisedString( "Money to complete repair" )+": %d",moneyCost));
					repairRightClickTimeMap.put(event.getPlayer(), System.currentTimeMillis() );
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
	
	@EventHandler
	public void onPlayerInteractStick( PlayerInteractEvent event ) {
		
		Craft c = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
		// if not in command of craft, don't process pilot tool clicks
		if(c==null)
			return;
		
		if ( event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK ) {
			Craft craft = CraftManager.getInstance().getCraftByPlayer( event.getPlayer() );
			
			if ( event.getItem() != null && event.getItem().getTypeId()==Settings.PilotTool ) {
				event.setCancelled(true);
				if ( craft != null ) {
					Long time = timeMap.get( event.getPlayer() );
					if ( time != null ) {
						long ticksElapsed = ( System.currentTimeMillis() - time ) / 50;

						// if the craft should go slower underwater, make time pass more slowly there
						if(craft.getType().getHalfSpeedUnderwater() && craft.getMinY()<craft.getW().getSeaLevel())
							ticksElapsed=ticksElapsed>>1;

						if ( Math.abs( ticksElapsed ) < craft.getType().getTickCooldown() ) {
							return;
						} 						
					}

					if ( MathUtils.playerIsWithinBoundingPolygon( craft.getHitBox(), craft.getMinX(), craft.getMinZ(), MathUtils.bukkit2MovecraftLoc( event.getPlayer().getLocation() ) ) ) {

						if ( event.getPlayer().hasPermission( "movecraft." + craft.getType().getCraftName() + ".move" ) ) {
							if( craft.getPilotLocked()==true ) {
								// right click moves up or down if using direct control
								int DY=1;
								if(event.getPlayer().isSneaking()) 
									DY=-1;
								
								// See if the player is holding down the mouse button and update the last right clicked info
								if(System.currentTimeMillis()-craft.getLastRightClick()<500) {
									craft.setLastDX(0);
									craft.setLastDY(DY);
									craft.setLastDZ(0);
									craft.setKeepMoving(true);
								} else {
									craft.setLastDX(0);
									craft.setLastDY(0);
									craft.setLastDZ(0);
									craft.setKeepMoving(false);
								}
								craft.setLastRightClick(System.currentTimeMillis());
	
								craft.translate( 0, DY, 0 );
								timeMap.put( event.getPlayer(), System.currentTimeMillis() );
								craft.setLastCruisUpdate(System.currentTimeMillis());
							} else {
								// Player is onboard craft and right clicking
								float rotation = ( float ) Math.PI * event.getPlayer().getLocation().getYaw() / 180f;
	
								float nx = -( float ) Math.sin( rotation );
								float nz = ( float ) Math.cos( rotation );
	
								int dx = ( Math.abs( nx ) >= 0.5 ? 1 : 0 ) * ( int ) Math.signum( nx );
								int dz = ( Math.abs( nz ) > 0.5 ? 1 : 0 ) * ( int ) Math.signum( nz );
								int dy;
	
								float p = event.getPlayer().getLocation().getPitch();
	
								dy = -( Math.abs( p ) >= 25 ? 1 : 0 )
										* ( int ) Math.signum( p );
	
								if ( Math.abs( event.getPlayer().getLocation().getPitch() ) >= 75 ) {
									dx = 0;
									dz = 0;
								}
								
								// See if the player is holding down the mouse button and update the last right clicked info
								if(System.currentTimeMillis()-craft.getLastRightClick()<500) {
									craft.setLastDX(dx);
									craft.setLastDY(dy);
									craft.setLastDZ(dz);
									craft.setKeepMoving(true);
								} else {
									craft.setLastDX(0);
									craft.setLastDY(0);
									craft.setLastDZ(0);
									craft.setKeepMoving(false);
								}
								craft.setLastRightClick(System.currentTimeMillis());
	
								craft.translate( dx, dy, dz );
								timeMap.put( event.getPlayer(), System.currentTimeMillis() );
								craft.setLastCruisUpdate(System.currentTimeMillis());
							}
						} else { 
							event.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "Insufficient Permissions" ) ) );
						}
					}
				}
			}
		}
		if ( event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK ) {
			if ( event.getItem() != null && event.getItem().getTypeId()==Settings.PilotTool ) {
				Craft craft = CraftManager.getInstance().getCraftByPlayer( event.getPlayer() );
				if( craft!=null ) {
					if ( craft.getPilotLocked()==false) {
						if ( event.getPlayer().hasPermission( "movecraft." + craft.getType().getCraftName() + ".move" ) && craft.getType().getCanDirectControl() ) {
							craft.setPilotLocked(true);
							craft.setPilotLockedX(event.getPlayer().getLocation().getBlockX()+0.5);
							craft.setPilotLockedY(event.getPlayer().getLocation().getY());
							craft.setPilotLockedZ(event.getPlayer().getLocation().getBlockZ()+0.5);
							event.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "Entering Direct Control Mode" ) ) );
							event.setCancelled(true);
							return;
						} else {
							event.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "Insufficient Permissions" ) ) );						
						}
					} else {
						craft.setPilotLocked(false);
						event.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "Leaving Direct Control Mode" ) ) );
						event.setCancelled(true);
						return;
					}
				}
			}
		}

	}

}
