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

package net.countercraft.movecraft.config;

import net.countercraft.movecraft.Movecraft;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

class ConfigManager {
	private final FileConfiguration configFile;

	public ConfigManager( FileConfiguration configFile ) {
		this.configFile = configFile;
	}

	public void loadConfig() {
		setupDefaults();

		Settings.DATA_BLOCKS = configFile.getIntegerList( "dataBlocks" );
		Settings.THREAD_POOL_SIZE = configFile.getInt( "ThreadPoolSize" );
		Settings.IGNORE_RESET = configFile.getBoolean( "safeReload" );

	}

	private void setupDefaults() {
		configFile.addDefault( "ThreadPoolSize", 5 );
		configFile.addDefault( "safeReload", false );
		List<Integer> dataBlockList = new ArrayList<Integer>();
		dataBlockList.add( 23 );
		dataBlockList.add( 25 );
		dataBlockList.add( 33 );
		dataBlockList.add( 44 );
		dataBlockList.add( 50 );
		dataBlockList.add( 53 );
		dataBlockList.add( 54 );
		dataBlockList.add( 55 );
		dataBlockList.add( 61 );
		dataBlockList.add( 62 );
		dataBlockList.add( 63 );
		dataBlockList.add( 64 );
		dataBlockList.add( 65 );
		configFile.addDefault( "dataBlocks", dataBlockList );
		configFile.options().copyDefaults( true );
		Movecraft.getInstance().saveConfig();
	}
}
