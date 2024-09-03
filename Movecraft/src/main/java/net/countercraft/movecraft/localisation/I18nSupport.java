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

import jakarta.inject.Inject;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.lifecycle.HostedService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.plugin.Plugin;
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
import java.util.logging.Logger;

public class I18nSupport implements HostedService {
    private static Properties languageFile;
    private final @NotNull Plugin plugin;
    private final @NotNull Logger logger;

    @Inject
    public I18nSupport(@NotNull Plugin plugin, @NotNull Logger logger){
        this.plugin = plugin;
        this.logger = logger;
    }



    @Override
    public void start() {
        String[] localisations = {"en", "cz", "nl", "fr"};
        for (String locale : localisations) {
            var file = new File("%s/localisation/movecraftlang_%s.properties".formatted(plugin.getDataFolder(), locale));
            if (!file.exists()) {
                plugin.saveResource("localisation/movecraftlang_%s.properties".formatted(locale), false);
            }
        }

        init();
    }

    private void init() {
        languageFile = new Properties();

        File localisationDirectory = new File(plugin.getDataFolder().getAbsolutePath() + "/localisation");

        if (!localisationDirectory.exists()) {
            localisationDirectory.mkdirs();
        }

        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(localisationDirectory.getAbsolutePath() + "/movecraftlang" + "_" + Settings.LOCALE + ".properties");
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Critical Error in Localisation System", e);
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
