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
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.MathUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;

public class PlayerListener implements Listener {
	private final HashMap<Player, BukkitTask> releaseEvents = new HashMap<Player, BukkitTask>();

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
			if ( !MathUtils.playerIsWithinBoundingPolygon( c.getHitBox(), c.getMinX(), c.getMinZ(), MathUtils.bukkit2MovecraftLoc( event.getPlayer().getLocation() ) ) ) {

				if ( !releaseEvents.containsKey( event.getPlayer() ) ) {
					event.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "Release - Player has left craft" ) ) );

					BukkitTask releaseTask = new BukkitRunnable() {

						@Override
						public void run() {
							CraftManager.getInstance().removeCraft( c );
						}

					}.runTaskLater( Movecraft.getInstance(), ( 20 * 15 ) );

					releaseEvents.put( event.getPlayer(), releaseTask );
				}

			} else if ( releaseEvents.containsKey( event.getPlayer() ) ) {
				releaseEvents.get( event.getPlayer() ).cancel();
				releaseEvents.remove( event.getPlayer() );
			}
		}
	}

	@EventHandler
	public void onPlayerHit( EntityDamageByEntityEvent event ) {
		if ( event.getEntity() instanceof Player && CraftManager.getInstance().getCraftByPlayer( ( Player ) event.getEntity() ) != null ) {
			CraftManager.getInstance().removeCraft( CraftManager.getInstance().getCraftByPlayer( ( Player ) event.getEntity() ) );
		}
	}

}
