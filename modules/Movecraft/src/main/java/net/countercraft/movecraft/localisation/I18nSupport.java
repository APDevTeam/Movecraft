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
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Level;

public final class I18nSupport {
    private static Properties languageFile;
    private static boolean hasOtherScript = false;
    private static final String[] otherScriptLangs = {"el", "ar", "ur", "he", "zh", "jp"};
    private static Movecraft movecraft = Movecraft.getInstance();
    public static void init() {
        languageFile = new Properties();

        File localisationDirectory = new File(movecraft.getDataFolder().getAbsolutePath() + "/localisation");

        if (!localisationDirectory.exists()) {
            localisationDirectory.mkdirs();
        }

        InputStream is = null;
        try {
            is = new FileInputStream(localisationDirectory.getAbsolutePath() + "/movecraftlang" + "_" + Settings.LOCALE + ".properties");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        if (is == null) {
            movecraft.getLogger().log(Level.SEVERE, "Critical Error in Localisation System");
            movecraft.getServer().shutdown();
            return;
        }

        try {
            languageFile.load(is);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        InputStream resource = Movecraft.getInstance().getResource("localisation/movecraftlang" + "_" + Settings.LOCALE + ".properties");
        Properties jarLangFile = new Properties();
        if (resource == null) {
            return;
        }
        try {
            jarLangFile.load(resource);
            resource.close();
        } catch (IOException e) {
            if (Settings.Debug)
                e.printStackTrace();
            return;
        }
        boolean updated = false;
        for (Object key : jarLangFile.keySet()) {
            if (languageFile.contains(key)) {
                continue;
            }
            languageFile.setProperty((String) key, (String) jarLangFile.get(key));
            updated = true;
        }
        if (!updated)
            return;
        try {
            OutputStream output = new FileOutputStream(localisationDirectory.getAbsolutePath() + "/movecraftlang" + "_" + Settings.LOCALE + ".properties");
            languageFile.store(output, null);
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
    public static boolean writtenFromRightToLeft(){
        return (boolean) languageFile.getOrDefault("writtenFromRightToLeft", false);
    }
    public static String getInternationalisedString(String key) {
        String ret;
        if (Arrays.binarySearch(otherScriptLangs, Settings.LOCALE) >= 0){
            try {
                ret = new String(languageFile.getProperty(key).getBytes(),"UTF-8");
            } catch (UnsupportedEncodingException e) {
                ret = languageFile.getProperty(key);
                e.printStackTrace();
            }
        } else {
            ret = languageFile.getProperty(key);
        }
        if (ret != null) {
            return ret;
        } else {
            return key;
        }
    }

}
