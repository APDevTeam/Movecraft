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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.logging.Level;

public class I18nSupport {
    private static Properties languageFile;

    public static void init() {
        languageFile = new Properties();

        File localisationDirectory = new File(Movecraft.getInstance().getDataFolder().getAbsolutePath() + "/localisation");

        if (!localisationDirectory.exists()) {
            localisationDirectory.mkdirs();
        }

        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(localisationDirectory.getAbsolutePath() + "/movecraftlang" + "_" + Settings.LOCALE + ".properties");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if (inputStream == null) {
            Movecraft.getInstance().getLogger().log(Level.SEVERE, "Critical Error in Localisation System");
            Movecraft.getInstance().getServer().shutdown();
            return;
        }

        try {
            languageFile.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String get(String key) {
        String ret = languageFile.getProperty(key);
        if (ret != null) {
            return ret;
        } else {
            return key;
        }

    }

    @Deprecated(forRemoval = true)
    public static String getInternationalisedString(String key) {
        return get(key);
    }

    @Contract("_ -> new")
    public static @NotNull TextComponent getInternationalisedComponent(String key){
        return Component.text(get(key));
    }
}
