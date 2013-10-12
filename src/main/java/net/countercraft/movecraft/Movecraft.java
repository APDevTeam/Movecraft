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

package net.countercraft.movecraft;

import net.countercraft.movecraft.async.AsyncManager;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.items.StorageChestItem;
import net.countercraft.movecraft.listener.BlockListener;
import net.countercraft.movecraft.listener.CommandListener;
import net.countercraft.movecraft.listener.InteractListener;
import net.countercraft.movecraft.listener.PlayerListener;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.metrics.MovecraftMetrics;  
import net.countercraft.movecraft.utils.MapUpdateManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Movecraft extends JavaPlugin {
	private static Movecraft instance;
	private Logger logger;
	private boolean shuttingDown;

	public void onDisable() {
		// Process the storage crates to disk
		StorageChestItem.saveToDisk();
		shuttingDown = true;
	}

	public void onEnable() {
		// Read in config
		this.saveDefaultConfig();
		Settings.LOCALE = getConfig().getString( "Locale" );
		// if the PilotTool is specified in the config.yml file, use it
		if(getConfig().getInt("PilotTool")!=0) {
			logger.log( Level.INFO, "Recognized PilotTool setting of: "+getConfig().getInt("PilotTool"));
			Settings.PilotTool=getConfig().getInt("PilotTool");
		} else {
			logger.log( Level.INFO, "No PilotTool setting, using default of 280");
		}
		// if the CompatibilityMode is specified in the config.yml file, use it. Otherwise set to false. - NOT IMPLEMENTED YET - Mark
		Settings.CompatibilityMode=getConfig().getBoolean("CompatibilityMode", false);
		

		if ( !new File( getDataFolder() + "/localisation/movecraftlang_en.properties" ).exists() ) {
			this.saveResource( "localisation/movecraftlang_en.properties", false );
		}
		I18nSupport.init();
		if ( shuttingDown && Settings.IGNORE_RESET ) {
			logger.log( Level.SEVERE, String.format( I18nSupport.getInternationalisedString( "Startup - Error - Reload error" ) ) );
			logger.log( Level.INFO, String.format( I18nSupport.getInternationalisedString( "Startup - Error - Disable warning for reload" ) ) );
			getPluginLoader().disablePlugin( this );
		} else {


			// Startup procedure
			AsyncManager.getInstance().runTaskTimer( this, 0, 1 );
			MapUpdateManager.getInstance().runTaskTimer( this, 0, 1 );

			CraftManager.getInstance();

			getServer().getPluginManager().registerEvents( new InteractListener(), this );
			getServer().getPluginManager().registerEvents( new CommandListener(), this );
			getServer().getPluginManager().registerEvents( new BlockListener(), this );
			getServer().getPluginManager().registerEvents( new PlayerListener(), this );

			StorageChestItem.readFromDisk();
			StorageChestItem.addRecipie();

			new MovecraftMetrics( CraftManager.getInstance().getCraftTypes().length );   

			logger.log( Level.INFO, String.format( I18nSupport.getInternationalisedString( "Startup - Enabled message" ), getDescription().getVersion() ) );
		}
	}

	@Override
	public void onLoad() {
		super.onLoad();
		instance = this;
		logger = getLogger();
	}

	public static Movecraft getInstance() {
		return instance;
	}
}
