package net.countercraft.movecraft.utils;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;

public class LegacyUtils {

    //Materials removed in 1.13
    public static Material BED_BLOCK = Material.BED_BLOCK;
    public static Material CARPET = Material.CARPET;
    public static Material CROPS = Material.CROPS;
    public static Material DAYLIGHT_DETECTOR_INVERTED = Material.DAYLIGHT_DETECTOR_INVERTED;
    public static Material DIODE = Material.DIODE;
    public static Material DIODE_BLOCK_OFF = Material.DIODE_BLOCK_OFF;
    public static Material DIODE_BLOCK_ON = Material.DIODE_BLOCK_ON;
    public static Material DOUBLE_PLANT = Material.DOUBLE_PLANT;
    public static Material FENCE = Material.FENCE;
    public static Material FIREBALL = Material.FIREBALL;
    public static Material GOLD_PLATE = Material.GOLD_PLATE;
    public static Material IRON_DOOR_BLOCK = Material.IRON_DOOR_BLOCK;
    public static Material IRON_FENCE = Material.IRON_FENCE;
    public static Material IRON_PLATE = Material.IRON_PLATE;
    public static Material LEAVES = Material.LEAVES;
    public static Material LEAVES_2 = Material.LEAVES_2;
    public static Material LOG = Material.LOG;
    public static Material LOG_2 = Material.LOG_2;
    public static Material LONG_GRASS = Material.LONG_GRASS;
    public static Material PISTON_BASE = Material.PISTON_BASE;
    public static Material PISTON_EXTENSION = Material.PISTON_EXTENSION;
    public static Material PISTON_STICKY_BASE = Material.PISTON_STICKY_BASE;
    public static Material RED_ROSE = Material.RED_ROSE;
    public static Material REDSTONE_COMPARATOR = Material.REDSTONE_COMPARATOR;
    public static Material REDSTONE_COMPARATOR_OFF = Material.REDSTONE_COMPARATOR_OFF;
    public static Material REDSTONE_COMPARATOR_ON = Material.REDSTONE_COMPARATOR_ON;
    public static Material REDSTONE_TORCH_ON = Material.REDSTONE_TORCH_ON;
    public static Material REDSTONE_TORCH_OFF = Material.REDSTONE_TORCH_OFF;
    public static Material SEEDS = Material.SEEDS;
    public static Material SMOOTH_STAIRS = Material.SMOOTH_STAIRS;
    public static Material STAINED_GLASS = Material.STAINED_GLASS;
    public static Material STAINED_GLASS_PANE = Material.STAINED_GLASS_PANE;
    public static Material STATIONARY_WATER = Material.STATIONARY_WATER;
    public static Material STATIONARY_LAVA = Material.STATIONARY_LAVA;
    public static Material STEP = Material.STEP;
    public static Material STONE_PLATE = Material.STONE_PLATE;
    public static Material SIGN_POST = Material.SIGN_POST;
    public static Material SUGAR_CANE_BLOCK = Material.SUGAR_CANE_BLOCK;
    public static Material THIN_GLASS = Material.THIN_GLASS;
    public static Material TRAP_DOOR = Material.TRAP_DOOR;
    public static Material WATER_LILY = Material.WATER_LILY;
    public static Material WEB = Material.WEB;
    public static Material WOOL = Material.WOOL;
    public static Material WOOD = Material.WOOD;
    public static Material WOOD_BUTTON = Material.WOOD_BUTTON;
    public static Material WOOD_DOOR = Material.WOOD_DOOR;
    public static Material WOOD_PLATE = Material.WOOD_PLATE;
    public static Material YELLOW_FLOWER = Material.YELLOW_FLOWER;

    //Sounds removed in 1.13
    public static Sound ENITIY_IRONGOLEM_DEATH = Sound.ENTITY_IRONGOLEM_DEATH;
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
