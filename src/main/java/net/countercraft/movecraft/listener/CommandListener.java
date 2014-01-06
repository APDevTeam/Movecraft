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
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandListener implements Listener {

	@EventHandler
	public void onCommand( PlayerCommandPreprocessEvent e ) {

		if ( e.getMessage().equalsIgnoreCase( "/release" ) ) {
			final Craft pCraft = CraftManager.getInstance().getCraftByPlayer( e.getPlayer() );

			if ( pCraft != null ) {
				CraftManager.getInstance().removeCraft( pCraft );
				//e.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "Player- Craft has been released" ) ) );
			} else {
				e.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "Player- Error - You do not have a craft to release!" ) ) );
			}

			e.setCancelled( true );
		}

	}

}
