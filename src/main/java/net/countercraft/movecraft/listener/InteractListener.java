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
import net.countercraft.movecraft.utils.MathUtils;
import net.countercraft.movecraft.utils.MovecraftLocation;
import net.countercraft.movecraft.utils.Rotation;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class InteractListener implements Listener {
	private static final Map<Player, Long> timeMap = new HashMap<Player, Long>();

	@EventHandler
	public void onPlayerInteract( PlayerInteractEvent event ) {

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
				String signText = sign.getLine( 0 );

				if ( signText == null ) {
					return;
				}

				if ( sign.getLine( 0 ).equals( "\\  ||  /" ) && sign.getLine( 1 ).equals( "==      ==" ) && sign.getLine( 2 ).equals( "/  ||  \\" ) ) {
					Craft craft = CraftManager.getInstance().getCraftByPlayer( event.getPlayer() );
					if ( craft != null ) {
						if ( event.getPlayer().hasPermission( "movecraft." + craft.getType().getCraftName() + ".rotate" ) ) {

							Long time = timeMap.get( event.getPlayer() );
							if ( time != null ) {
								long ticksElapsed = ( System.currentTimeMillis() - time ) / 50;
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
			}
		}
	}

	private void onSignRightClick( PlayerInteractEvent event ) {
		Sign sign = ( Sign ) event.getClickedBlock().getState();
		String signText = sign.getLine( 0 );

		if ( signText == null ) {
			return;
		}
		
		// don't process commands if this is a pilot tool click, do that below
		if ( event.getItem() != null && event.getItem().getTypeId()==Settings.PilotTool ) {
			Craft c = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
			if(c!=null)
				return;
		}


		if ( getCraftTypeFromString( sign.getLine( 0 ) ) != null ) {

			// Valid sign prompt for ship command.
			if ( event.getPlayer().hasPermission( "movecraft." + sign.getLine( 0 ) + ".pilot" ) ) {
				// Attempt to run detection
				Location loc = event.getClickedBlock().getLocation();
				MovecraftLocation startPoint = new MovecraftLocation( loc.getBlockX(), loc.getBlockY(), loc.getBlockZ() );
				final Craft c = new Craft( getCraftTypeFromString( sign.getLine( 0 ) ), loc.getWorld() );
				
				if(c.getType().getCruiseOnPilot()==true) {
					c.detect( null, startPoint );
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
						c.detect( event.getPlayer(), startPoint );
					} else {
						Craft oldCraft=CraftManager.getInstance().getCraftByPlayer( event.getPlayer() );
						CraftManager.getInstance().removeCraft( oldCraft );
						c.detect( event.getPlayer(), startPoint );
					}
				}
				
				event.setCancelled( true );
			} else {
			event.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "Insufficient Permissions" ) ) );

			}

		} else if ( sign.getLine( 0 ).equalsIgnoreCase( "[helm]" ) ) {
			sign.setLine( 0, "\\  ||  /" );
			sign.setLine( 1, "==      ==" );
			sign.setLine( 2, "/  ||  \\" );
			sign.update( true );
			event.setCancelled( true );
		} else if ( sign.getLine( 0 ).equals( "\\  ||  /" ) && sign.getLine( 1 ).equals( "==      ==" ) && sign.getLine( 2 ).equals( "/  ||  \\" ) ) {
			Craft craft = CraftManager.getInstance().getCraftByPlayer( event.getPlayer() );
			if ( craft != null ) {
				if ( event.getPlayer().hasPermission( "movecraft." + craft.getType().getCraftName() + ".rotate" ) ) {
					Long time = timeMap.get( event.getPlayer() );
					if ( time != null ) {
						long ticksElapsed = ( System.currentTimeMillis() - time ) / 50;
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

		} else if ( sign.getLine( 0 ).equalsIgnoreCase( "Cruise: OFF")) {
			if(CraftManager.getInstance().getCraftByPlayer( event.getPlayer() )!=null){
				Craft c = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
				if(c.getType().getCanCruise()) {
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
		} else if ( sign.getLine( 0 ).equalsIgnoreCase( "Cruise: ON")) {
			if(CraftManager.getInstance().getCraftByPlayer( event.getPlayer() )!=null)
				if(CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).getType().getCanCruise()) {
					sign.setLine( 0, "Cruise: OFF" );
					sign.update( true );
					CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).setCruising(false);
				}
		} else if ( sign.getLine( 0 ).equalsIgnoreCase("Teleport:")) {
			if(CraftManager.getInstance().getCraftByPlayer( event.getPlayer() )!=null) {
				String[] numbers = sign.getLine( 1 ).split(",");
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
		} else if ( sign.getLine( 0 ).equalsIgnoreCase( "Release")) {
			if(CraftManager.getInstance().getCraftByPlayer( event.getPlayer() )!=null) {
				Craft oldCraft=CraftManager.getInstance().getCraftByPlayer( event.getPlayer() );
				CraftManager.getInstance().removeCraft( oldCraft );
			}
		} else if ( sign.getLine( 0 ).equalsIgnoreCase("Move:")) {
			if(CraftManager.getInstance().getCraftByPlayer( event.getPlayer() )!=null) {
				Long time = timeMap.get( event.getPlayer() );
				if ( time != null ) {
					long ticksElapsed = ( System.currentTimeMillis() - time ) / 50;
					if ( Math.abs( ticksElapsed ) < CraftManager.getInstance().getCraftByPlayer( event.getPlayer() ).getType().getTickCooldown() ) {
						event.setCancelled( true );
						return;
					}
				}
				String[] numbers = sign.getLine( 1 ).split(",");
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
					}
				} else {
					event.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "Insufficient Permissions" ) ) );
				}
			}
		} else  if ( sign.getLine( 0 ).equalsIgnoreCase("RMove:")) {
			if(CraftManager.getInstance().getCraftByPlayer( event.getPlayer() )!=null) {
				Long time = timeMap.get( event.getPlayer() );
				if ( time != null ) {
					long ticksElapsed = ( System.currentTimeMillis() - time ) / 50;
					if ( Math.abs( ticksElapsed ) < CraftManager.getInstance().getCraftByPlayer( event.getPlayer() ).getType().getTickCooldown() ) {
						event.setCancelled( true );
						return;
					}
				}
				String[] numbers = sign.getLine( 1 ).split(",");
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
					}
				} else {
					event.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "Insufficient Permissions" ) ) );
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
