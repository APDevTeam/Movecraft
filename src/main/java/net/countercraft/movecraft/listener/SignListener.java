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

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.CraftType;
import net.countercraft.movecraft.localisation.L18nSupport;
import net.countercraft.movecraft.utils.MathUtils;
import net.countercraft.movecraft.utils.MovecraftLocation;
import net.countercraft.movecraft.utils.Rotation;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class SignListener implements Listener{

	@EventHandler
	public void onPlayerInteract ( PlayerInteractEvent event ) {

		if ( event.getAction() == Action.RIGHT_CLICK_BLOCK ) {
			Material m = event.getClickedBlock().getType();
			if ( m.equals( Material.SIGN_POST)  || m.equals( Material.WALL_SIGN ) ){
				onSignRightClick( event );
			}
		} else if ( event.getAction() == Action.LEFT_CLICK_BLOCK ) {
			Material m = event.getClickedBlock().getType();
			if ( m.equals( Material.SIGN_POST)  || m.equals( Material.WALL_SIGN ) ){
				Sign sign = ( Sign ) event.getClickedBlock().getState();
				if ( sign.getLine( 0 ).equals( "\\  ||  /" ) && sign.getLine( 1 ).equals(  "==      ==" ) && sign.getLine( 2 ).equals(  "//  ||  \\" )) {
					CraftManager.getInstance().getCraftByPlayer( event.getPlayer() ).rotate( Rotation.ANTICLOCKWISE, MathUtils.bukkit2MovecraftLoc( sign.getLocation() ) );
				}
			}
		}
	}

	private void onSignRightClick ( PlayerInteractEvent event ) {
		Sign sign = ( Sign ) event.getClickedBlock().getState();


		if ( getCraftTypeFromString( sign.getLine( 0 ) ) != null ) {

			// Valid sign prompt for ship command.

			// Attempt to run detection
			Location loc = event.getClickedBlock().getLocation();
			MovecraftLocation startPoint = new MovecraftLocation( loc.getBlockX(), loc.getBlockY(), loc.getBlockZ() );
			Craft c = new Craft( getCraftTypeFromString( sign.getLine( 0 ) ), loc.getWorld() );

			if ( CraftManager.getInstance().getCraftByPlayer( event.getPlayer() ) == null ) {
				c.detect( event.getPlayer().getDisplayName(), startPoint );
			} else {
				event.getPlayer().sendMessage( String.format( L18nSupport.getInternationalisedString( "Player - Error - Already piloting craft" ) ) );
			}

			event.setCancelled( true );

		} else if ( sign.getLine( 0 ).equalsIgnoreCase( "[helm]" ) ) {
			sign.setLine( 0, "\\  ||  /" );
			sign.setLine( 1, "==      ==" );
			sign.setLine( 2, "/  ||  \\" );
			sign.update( true );
			event.setCancelled( true );
		} else if ( sign.getLine( 0 ).equals( "\\  ||  /" ) && sign.getLine( 1 ).equals(  "==      ==" ) && sign.getLine( 2 ).equals(  "/  ||  \\" )) {
			CraftManager.getInstance().getCraftByPlayer( event.getPlayer() ).rotate( Rotation.CLOCKWISE, MathUtils.bukkit2MovecraftLoc( sign.getLocation() ) );
			event.setCancelled( true );
		}


	}

	private CraftType getCraftTypeFromString ( String s ) {
		for ( CraftType t : CraftManager.getInstance().getCraftTypes() ) {
			if(s.equalsIgnoreCase( t.getCraftName() )){
				return t;
			}
		}

		return null;
	}

}
