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

package net.countercraft.movecraft.async;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Level;

public abstract class AsyncTask extends BukkitRunnable {
	private final Craft craft;

	protected AsyncTask( Craft c ) {
		craft = c;
	}

	public void run() {
		try {
			excecute();
			AsyncManager.getInstance().submitCompletedTask( this );
		} catch ( Exception e ) {
			Movecraft.getInstance().getLogger().log( Level.SEVERE, String.format( I18nSupport.getInternationalisedString( "Internal - Error - Proccessor thread encountered an error" ) ) );
			e.printStackTrace();
		}
	}

	protected abstract void excecute();

	protected Craft getCraft() {
		return craft;
	}
}
