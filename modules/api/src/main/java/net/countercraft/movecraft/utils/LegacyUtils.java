package net.countercraft.movecraft.utils;

import net.countercraft.movecraft.config.Settings;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;

public class LegacyUtils {

    public static final Material WALL_SIGN = !Settings.is1_14 ? Material.WALL_SIGN : null;
    //Materials removed in 1.13
    public static final Material BED_BLOCK = Settings.IsLegacy ? Material.BED_BLOCK : null;
    public static final Material CARPET = Settings.IsLegacy ? Material.CARPET : null;
    public static final Material CROPS = Settings.IsLegacy ? Material.CROPS : null;
    public static final Material DAYLIGHT_DETECTOR_INVERTED = Settings.IsLegacy ? Material.DAYLIGHT_DETECTOR_INVERTED : null;
    public static final Material DIODE = Settings.IsLegacy ? Material.DIODE : null;
    public static final Material DIODE_BLOCK_OFF = Settings.IsLegacy ? Material.DIODE_BLOCK_OFF : null;
    public static final Material DIODE_BLOCK_ON = Settings.IsLegacy ? Material.DIODE_BLOCK_ON : null;
    public static final Material DOUBLE_PLANT = Settings.IsLegacy ? Material.DOUBLE_PLANT : null;
    public static final Material FENCE = Settings.IsLegacy ? Material.FENCE : null;
    public static final Material FIREBALL = Settings.IsLegacy ? Material.FIREBALL : null;
    public static final Material GOLD_PLATE = Settings.IsLegacy ? Material.GOLD_PLATE : null;
    public static final Material IRON_DOOR_BLOCK = Settings.IsLegacy ? Material.IRON_DOOR_BLOCK : null;
    public static final Material IRON_FENCE = Settings.IsLegacy ? Material.IRON_FENCE : null;
    public static final Material IRON_PLATE = Settings.IsLegacy ? Material.IRON_PLATE : null;
    public static final Material LEAVES = Settings.IsLegacy ? Material.LEAVES : null;
    public static final Material LEAVES_2 = Settings.IsLegacy ? Material.LEAVES_2 : null;
    public static final Material LOG = Settings.IsLegacy ? Material.LOG : null;
    public static final Material LOG_2 = Settings.IsLegacy ? Material.LOG_2 : null;
    public static final Material LONG_GRASS = Settings.IsLegacy ? Material.LONG_GRASS : null;
    public static final Material PISTON_BASE = Settings.IsLegacy ? Material.PISTON_BASE : null;
    public static final Material PISTON_EXTENSION = Settings.IsLegacy ? Material.PISTON_EXTENSION : null;
    public static final Material PISTON_STICKY_BASE = Settings.IsLegacy ? Material.PISTON_STICKY_BASE : null;
    public static final Material PORTAL = Settings.IsLegacy ? Material.PORTAL : null;
    public static final Material RED_ROSE = Settings.IsLegacy ? Material.RED_ROSE : null;
    public static final Material REDSTONE_COMPARATOR = Settings.IsLegacy ? Material.REDSTONE_COMPARATOR : null;
    public static final Material REDSTONE_COMPARATOR_OFF = Settings.IsLegacy ? Material.REDSTONE_COMPARATOR_OFF : null;
    public static final Material REDSTONE_COMPARATOR_ON = Settings.IsLegacy ? Material.REDSTONE_COMPARATOR_ON : null;
    public static final Material REDSTONE_TORCH_ON = Settings.IsLegacy ? Material.REDSTONE_TORCH_ON : null;
    public static final Material REDSTONE_TORCH_OFF = Settings.IsLegacy ? Material.REDSTONE_TORCH_OFF : null;
    public static final Material SEEDS = Settings.IsLegacy ? Material.SEEDS : null;
    public static final Material SMOOTH_STAIRS = Settings.IsLegacy ? Material.SMOOTH_STAIRS : null;
    public static final Material STAINED_GLASS = Settings.IsLegacy ? Material.STAINED_GLASS : null;
    public static final Material STAINED_GLASS_PANE = Settings.IsLegacy ? Material.STAINED_GLASS_PANE : null;
    public static final Material STATIONARY_WATER = Settings.IsLegacy ? Material.STATIONARY_WATER : null;
    public static final Material STATIONARY_LAVA = Settings.IsLegacy ? Material.STATIONARY_LAVA : null;
    public static final Material STEP = Settings.IsLegacy ? Material.STEP : null;
    public static final Material STONE_PLATE = Settings.IsLegacy ? Material.STONE_PLATE : null;
    public static final Material SIGN_POST = Settings.IsLegacy ? Material.SIGN_POST : null;
    public static final Material SUGAR_CANE_BLOCK = Settings.IsLegacy ? Material.SUGAR_CANE_BLOCK : null;
    public static final Material THIN_GLASS = Settings.IsLegacy ? Material.THIN_GLASS : null;
    public static final Material TRAP_DOOR = Settings.IsLegacy ? Material.TRAP_DOOR : null;
    public static final Material WATER_LILY = Settings.IsLegacy ? Material.WATER_LILY : null;
    public static final Material WEB = Settings.IsLegacy ? Material.WEB : null;
    public static final Material WOOL = Settings.IsLegacy ? Material.WOOL : null;
    public static final Material WOOD = Settings.IsLegacy ? Material.WOOD : null;
    public static final Material WOOD_BUTTON = Settings.IsLegacy ? Material.WOOD_BUTTON : null;
    public static final Material WOOD_DOOR = Settings.IsLegacy ? Material.WOOD_DOOR : null;
    public static final Material WOOD_PLATE = Settings.IsLegacy ? Material.WOOD_PLATE : null;
    public static final Material YELLOW_FLOWER = Settings.IsLegacy ? Material.YELLOW_FLOWER : null;

    //Sounds removed in 1.13
    public static Sound ENITIY_IRONGOLEM_DEATH = Settings.IsLegacy && !Settings.IsPre1_9 ? Sound.ENTITY_IRONGOLEM_DEATH : null;
    public static Sound IRONGOLEM_DEATH = Settings.IsPre1_9 ? Sound.valueOf("IRONGOLEM_DEATH") : null;
    //Methods removed in 1.13
    public static Material getMaterial(int id){
        try {
            return Material.getMaterial(id);
        } catch (IllegalArgumentException e){
            return null;
        }
    }




    private static Material get(String name) {

        try {
            return Material.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static void setTypeIdAndData(Block b, int id, byte data, boolean b1){
        b.setTypeIdAndData(id, data, b1);
    }
    public static void setData(Block b, byte data){
        b.setData(data);
    }

    private String getColorFromData(byte data){
        String color = "";
        switch (data){
            case 0 :
                color = "WHITE";
                break;
            case 1 :
                color = "ORANGE";
                break;
            case 2 :
                color = "MAGENTA";
                break;
            case 3 :
                color = "LIGHT_BLUE";
                break;
            case 4 :
                color = "YELLOW";
                break;
            case 5 :
                color = "LIME";
                break;
            case 6 :
                color = "PINK";
                break;
            case 7 :
                color = "GRAY";
                break;
            case 8 :
                color = "LIGHT_GRAY";
                break;
        }
        return color;
    }
}
