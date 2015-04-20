package net.countercraft.movecraft.utils;

/**
 *
 * @author mwkaicz <mwkaicz@gmail.com>
 */
public class TownyWorldHeightLimits {
    public static final int DEFAULT_WORLD_MIN = -1;
    public static final int DEFAULT_WORLD_MAX = -1;
    public static final int DEFAULT_TOWN_ABOVE = 96;
    public static final int DEFAULT_TOWN_UNDER = 255;
    public int world_min = DEFAULT_WORLD_MIN;
    public int world_max = DEFAULT_WORLD_MAX;
    public int above_town = DEFAULT_TOWN_ABOVE;
    public int under_town = DEFAULT_TOWN_UNDER;
    
    public boolean validate(int y, int spawnY){
        
        if (world_min > -1 && world_max > -1){
            if (y > world_min  && y < world_max){
                return false;
            }
        }else if (world_min > -1){
            if (y > world_min){
                return false;
            }
        }else if (world_max > -1){
            if (y < world_max){
                return false;
            }
        }
        
        if (above_town > -1 && under_town > -1){
            if (y < spawnY + above_town && y > spawnY - under_town){
                return false;
            }
        }else if (above_town > -1){
            if (y >= spawnY && y < spawnY + above_town){
                return false;
            }
        }else if (under_town > -1){
            if (y <=spawnY && y > spawnY - under_town){
                return false;
            }
        }
        
        return true;
        
        //return !((y > world_min && y < world_max) || (y < spawnY + above_town && y > spawnY - under_town ) );
    }
}
