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

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

class ConfigManager {
    private final FileConfiguration configFile;

    public ConfigManager(FileConfiguration configFile) {
        this.configFile = configFile;
    }

    public void loadConfig() {
        setupDefaults();
        Settings.DATA_BLOCKS = (List<Material>) configFile.getList("dataBlocks");
        Settings.THREAD_POOL_SIZE = configFile.getInt("ThreadPoolSize");
        Settings.IGNORE_RESET = configFile.getBoolean("safeReload");

    }

    private void setupDefaults() {
        configFile.addDefault("ThreadPoolSize", 5);
        configFile.addDefault("safeReload", false);
        List<Material> dataBlockList = new ArrayList<>();
        dataBlockList.add(Material.DISPENSER);
        dataBlockList.add(Material.NOTE_BLOCK);
        dataBlockList.add(Material.PISTON_BASE);
        dataBlockList.add(Material.STEP);
        dataBlockList.add(Material.TORCH);
        dataBlockList.add(Material.WOOD_STAIRS);
        dataBlockList.add(Material.CHEST);
        dataBlockList.add(Material.REDSTONE_WIRE);
        dataBlockList.add(Material.FURNACE);
        dataBlockList.add(Material.BURNING_FURNACE);
        dataBlockList.add(Material.SIGN_POST);
        dataBlockList.add(Material.WOOD_DOOR);
        dataBlockList.add(Material.LADDER);
        configFile.addDefault("dataBlocks", dataBlockList);
        configFile.options().copyDefaults(true);
        //Movecraft.getInstance().saveConfig();
    }
}
