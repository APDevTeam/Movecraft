package net.countercraft.movecraft.repair;

import net.countercraft.movecraft.Movecraft;
import org.bukkit.Material;

import java.util.HashSet;
import java.util.Set;

public class RepairUtils {
    private static Set<Material> COLORED_BLOCKS = new HashSet<>();

    static {
        COLORED_BLOCKS.add(Material.WOOL);
        COLORED_BLOCKS.add(Material.STAINED_GLASS_PANE);
        COLORED_BLOCKS.add(Material.STAINED_GLASS);
        COLORED_BLOCKS.add(Material.STAINED_CLAY);
        COLORED_BLOCKS.add(Material.CARPET);
        if (Movecraft.getInstance().getServer().getVersion().split(".")[1].equals("12")) {
            COLORED_BLOCKS.add(Material.getMaterial("CONCRETE"));
            COLORED_BLOCKS.add(Material.getMaterial("CONCRETE_POWDER"));
        }
        COLORED_BLOCKS.add(Material.BED_BLOCK);
    }

    public static String specificName(Material type, int data){
        String ret = "";
        if (COLORED_BLOCKS.contains(type)){
            switch (data){
                case 0:
                    ret += "white ";
                    break;
                case 1:
                    ret += "orange ";
                    break;
                case 2:
                    ret += "magenta ";
                    break;
                case 3:
                    ret += "light blue ";
                    break;
                case 4:
                    ret += "yellow ";
                    break;
                case 5:
                    ret += "lime ";
                    break;
                case 6:
                    ret += "pink ";
                    break;
                case 7:
                    ret += "gray ";
                    break;
                case 8:
                    ret += "light gray ";
                    break;
                case 9:
                    ret += "cyan ";
                    break;
                case 10:
                    ret += "purple ";
                    break;
                case 11:
                    ret += "blue ";
                    break;
                case 12:
                    ret += "brown ";
                    break;
                case 13:
                    ret += "green ";
                    break;
                case 14:
                    ret += "red ";
                    break;
                case 15:
                    ret += "black ";
                    break;
            }
        }
        else if (type.equals(Material.WOOD)){
            switch (data){
                case 0:
                    ret += "oak ";
                    break;
                case 1:
                    ret += "spruce ";
                    break;
                case 2:
                    ret += "birch ";
                    break;
                case 3:
                    ret += "jungle ";
                    break;
                case 4:
                    ret += "acacia ";
                    break;
                case 5:
                    ret += "dark oak ";
                    break;
            }
        }
        else if (type.equals(Material.LOG)){
            switch (data){
                case 0:
                    ret += "oak ";
                    break;
                case 1:
                    ret += "spruce ";
                    break;
                case 2:
                    ret += "birch ";
                    break;
                case 3:
                    ret += "jungle ";
                    break;
            }
        }
        else if (type.equals(Material.LOG_2)){
            switch (data){
                case 0:
                    ret += "acacia ";
                    break;
                case 1:
                    ret += "dark oak ";
                    break;
            }
        }
        else if (type.equals(Material.STEP)){
            switch (data){
                case 0:
                    ret += "stone ";
                    break;
                case 1:
                    ret += "sandstone ";
                    break;
                case 2:
                    ret += "cobblestone ";
                    break;
                case 3:
                    ret += "brick ";
                    break;
                case 4:
                    ret += "stonebrick ";
                    break;
                case 5:
                    ret += "gray ";
                    break;
                case 6:
                    ret += "nether brick ";
                    break;
                case 7:
                    ret += "quartz ";
                    break;
            }
        }
        else if (type.equals(Material.COAL)){
            if (data == 1) {
                ret += "char";
            }
        }
        else if (type.equals(Material.WOOD_STEP)){
            switch (data){
                case 0:
                    ret += "oak ";
                    break;
                case 1:
                    ret += "spruce ";
                    break;
                case 2:
                    ret += "birch ";
                    break;
                case 3:
                    ret += "jungle ";
                    break;
                case 4:
                    ret += "acacia ";
                    break;
                case 5:
                    ret += "dark oak ";
                    break;
            }
        }
        ret += type.name().toLowerCase().replace("_", " ");
        return ret;
    }

}
