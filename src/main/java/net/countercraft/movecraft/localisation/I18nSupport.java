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

package net.countercraft.movecraft.localisation;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.config.Settings;

import java.io.*;
import java.util.Properties;
import java.util.logging.Level;

public class I18nSupport {
	private static Properties languageFile;

	public static void init() {
		languageFile = new Properties();

		File localisationDirectory = new File( Movecraft.getInstance().getDataFolder().getAbsolutePath() + "/localisation" );

		if ( !localisationDirectory.exists() ) {
			localisationDirectory.mkdirs();
		}

		InputStream is = null;
		try {
			is = new FileInputStream( localisationDirectory.getAbsolutePath() + "/movecraftlang" + "_" + Settings.LOCALE + ".properties" );
		} catch ( FileNotFoundException e ) {
			e.printStackTrace();
		}

		if ( is == null ) {
			Movecraft.getInstance().getLogger().log( Level.SEVERE, "Critical Error in Localisation System" );
			Movecraft.getInstance().getServer().shutdown();
			return;
		}

		try {
			languageFile.load( is );
			is.close();
		} catch ( IOException e ) {
			e.printStackTrace();
		}


	}

	public static String getInternationalisedString( String key ) {
		return languageFile.getProperty( key );
	}

}
