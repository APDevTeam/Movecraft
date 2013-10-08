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

import java.util.HashMap;
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


		if ( getCraftTypeFromString( sign.getLine( 0 ) ) != null ) {

			// Valid sign prompt for ship command.
			if ( event.getPlayer().hasPermission( "movecraft." + sign.getLine( 0 ) + ".pilot" ) ) {
				// Attempt to run detection
				Location loc = event.getClickedBlock().getLocation();
				MovecraftLocation startPoint = new MovecraftLocation( loc.getBlockX(), loc.getBlockY(), loc.getBlockZ() );
				Craft c = new Craft( getCraftTypeFromString( sign.getLine( 0 ) ), loc.getWorld() );

				if ( CraftManager.getInstance().getCraftByPlayer( event.getPlayer() ) == null ) {
					c.detect( event.getPlayer().getName(), startPoint );
				} else {
					event.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "Player - Error - Already piloting craft" ) ) );
				}

				event.setCancelled( true );
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
		if ( event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK ) {
			//if ( event.getItem() != null && event.getItem().getType().equals( Material.STICK ) ) {
			if ( event.getItem() != null && event.getItem().getTypeId()==Settings.PilotTool ) {
			
				Craft craft = CraftManager.getInstance().getCraftByPlayer( event.getPlayer() );
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

							craft.translate( dx, dy, dz );
							timeMap.put( event.getPlayer(), System.currentTimeMillis() );
						}
					}
				}
			}
		}
	}

}
