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
import sun.misc.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Properties;

public class L18nSupport {
	public static Properties languageFile = new Properties();

	public static void init () {
		languageFile = new Properties();

		InputStream is = null;
		is = Movecraft.getInstance().getClass().getResourceAsStream("/localization/" + Settings.LOCALE + ".properties");

		if (is == null) {
			is = Movecraft.getInstance().getClass().getResourceAsStream("/localization/en_US.properties");
		}

		try {
			languageFile.load( is );
			is.close();
		} catch ( IOException e ) {
			e.printStackTrace();
		}


	}

	public static String getInternationalisedString ( String key ) {
		return languageFile.getProperty( key );
	}

}
