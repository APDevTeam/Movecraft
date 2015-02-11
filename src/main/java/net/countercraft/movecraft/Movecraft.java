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
import at.pavlov.cannons.Cannons;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

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
import net.countercraft.movecraft.utils.MovecraftLocation;

import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Movecraft extends JavaPlugin {
	private static Movecraft instance;
	private static WorldGuardPlugin worldGuardPlugin;
	private static Cannons cannonsPlugin=null;
	private Logger logger;
	private boolean shuttingDown;
	public HashMap<MovecraftLocation, Long> blockFadeTimeMap = new HashMap<MovecraftLocation, Long>();
	public HashMap<MovecraftLocation, Integer> blockFadeTypeMap = new HashMap<MovecraftLocation, Integer>();
	public HashMap<MovecraftLocation, Boolean> blockFadeWaterMap = new HashMap<MovecraftLocation, Boolean>();
	public HashMap<MovecraftLocation, World> blockFadeWorldMap = new HashMap<MovecraftLocation, World>();

	public void onDisable() {
		// Process the storage crates to disk
		if(Settings.DisableCrates==false)
			StorageChestItem.saveToDisk();
		shuttingDown = true;
	}

	public void onEnable() {
		// Read in config
		this.saveDefaultConfig();
		Settings.LOCALE = getConfig().getString("Locale");
		Settings.DisableCrates = getConfig().getBoolean("DisableCrates", false);
		Settings.DisableSpillProtection = getConfig().getBoolean("DisableSpillProtection", false);
		// if the PilotTool is specified in the config.yml file, use it
		if (getConfig().getInt("PilotTool") != 0) {
			logger.log(Level.INFO, "Recognized PilotTool setting of: "
					+ getConfig().getInt("PilotTool"));
			Settings.PilotTool = getConfig().getInt("PilotTool");
		} else {
			logger.log(Level.INFO, "No PilotTool setting, using default of 280");
		}
		// if the CompatibilityMode is specified in the config.yml file, use it.
		// Otherwise set to false.
		Settings.CompatibilityMode = getConfig().getBoolean("CompatibilityMode", false);
		if(Settings.CompatibilityMode==false) {
			try {
				 	Class.forName( "net.minecraft.server.v1_8_R1.Chunk" );
				} catch( ClassNotFoundException e ) {
					Settings.CompatibilityMode=true;
					logger.log(Level.INFO, "WARNING: CompatibilityMode was set to false, but required build-specific classes were not found. FORCING COMPATIBILITY MODE");
				}
		}
		logger.log(Level.INFO, "CompatiblityMode is set to "+Settings.CompatibilityMode);
		Settings.SinkRateTicks = getConfig().getDouble("SinkRateTicks", 20.0);
		Settings.SinkCheckTicks = getConfig().getDouble("SinkCheckTicks", 100.0);
		Settings.TracerRateTicks = getConfig().getDouble("TracerRateTicks", 5.0);
		Settings.ManOverBoardTimeout = getConfig().getInt("ManOverBoardTimeout", 30);
		Settings.FireballLifespan = getConfig().getInt("FireballLifespan", 6);
		Settings.RequireCreatePerm = getConfig().getBoolean("RequireCreatePerm", false);
		Settings.TNTContactExplosives = getConfig().getBoolean("TNTContactExplosives", true);
		Settings.FadeWrecksAfter = getConfig().getInt("FadeWrecksAfter", 0);
		
		//load up WorldGuard if it's present
		Plugin wGPlugin=getServer().getPluginManager().getPlugin("WorldGuard");
		if (wGPlugin == null || !(wGPlugin instanceof WorldGuardPlugin)) {
			logger.log(Level.INFO, "Movecraft did not find a compatible version of WorldGuard. Disabling WorldGuard integration");			
		} else {
			logger.log(Level.INFO, "Found a compatible version of WorldGuard. Enabling WorldGuard integration");			
			Settings.WorldGuardBlockMoveOnBuildPerm = getConfig().getBoolean("WorldGuardBlockMoveOnBuildPerm", false);
			Settings.WorldGuardBlockSinkOnPVPPerm = getConfig().getBoolean("WorldGuardBlockSinkOnPVPPerm", false);
			logger.log(Level.INFO, "Settings: WorldGuardBlockMoveOnBuildPerm - "+Settings.WorldGuardBlockMoveOnBuildPerm+", WorldGuardBlockSinkOnPVPPerm - "+Settings.WorldGuardBlockSinkOnPVPPerm);			
		}
		worldGuardPlugin=(WorldGuardPlugin)wGPlugin;
		
		Plugin plug = getServer().getPluginManager().getPlugin("Cannons");
        if (plug != null && plug instanceof Cannons) {
            cannonsPlugin = (Cannons) plug;
			logger.log(Level.INFO, "Found a compatible version of Cannons. Enabling Cannons integration");			
        }
        
		if (!new File(getDataFolder()
				+ "/localisation/movecraftlang_en.properties").exists()) {
			this.saveResource("localisation/movecraftlang_en.properties", false);
		}
		if (!new File(getDataFolder()
				+ "/types/airship.craft").exists()) {
			this.saveResource("types/airship.craft", false);
		}
		if (!new File(getDataFolder()
				+ "/types/airskiff.craft").exists()) {
			this.saveResource("types/airskiff.craft", false);
		}
		I18nSupport.init();
		if (shuttingDown && Settings.IGNORE_RESET) {
			logger.log(
					Level.SEVERE,
					String.format(I18nSupport
							.getInternationalisedString("Startup - Error - Reload error")));
			logger.log(
					Level.INFO,
					String.format(I18nSupport
							.getInternationalisedString("Startup - Error - Disable warning for reload")));
			getPluginLoader().disablePlugin(this);
		} else {

			// Startup procedure
			AsyncManager.getInstance().runTaskTimer(this, 0, 1);
			MapUpdateManager.getInstance().runTaskTimer(this, 0, 1);

			CraftManager.getInstance();

			getServer().getPluginManager().registerEvents(
					new InteractListener(), this);
//			getServer().getPluginManager().registerEvents(
//					new CommandListener(), this);
			this.getCommand("release").setExecutor(new CommandListener());
			this.getCommand("pilot").setExecutor(new CommandListener());
			this.getCommand("rotateleft").setExecutor(new CommandListener());
			this.getCommand("rotateright").setExecutor(new CommandListener());
			this.getCommand("cruise").setExecutor(new CommandListener());
			this.getCommand("cruiseoff").setExecutor(new CommandListener());
			this.getCommand("craftreport").setExecutor(new CommandListener());
			this.getCommand("manoverboard").setExecutor(new CommandListener());
			
			getServer().getPluginManager().registerEvents(new BlockListener(),
					this);
			getServer().getPluginManager().registerEvents(new PlayerListener(),
					this);
			
			if(Settings.DisableCrates==false) {
				StorageChestItem.readFromDisk();
				StorageChestItem.addRecipie();
			}

		 	new MovecraftMetrics(CraftManager.getInstance().getCraftTypes().length );
			

			logger.log(Level.INFO, String.format(I18nSupport
					.getInternationalisedString("Startup - Enabled message"),
					getDescription().getVersion()));
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
	
	public WorldGuardPlugin getWorldGuardPlugin() {
		return worldGuardPlugin;
	}
	
	public Cannons getCannonsPlugin() {
		return cannonsPlugin;
	}
}
