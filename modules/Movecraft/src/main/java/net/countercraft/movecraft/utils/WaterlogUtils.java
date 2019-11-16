package net.countercraft.movecraft.utils;

import net.countercraft.movecraft.MovecraftLocation;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;

public class WaterlogUtils {
    private WaterlogUtils(){

    }
    public static boolean adjacentToWater(World world, MovecraftLocation ml){
        for (MovecraftLocation shift : SHIFTS){
            final MovecraftLocation test = ml.add(shift);
            final Block testBlock = test.toBukkit(world).getBlock();

            if (testBlock.getType() == Material.WATER){
                return true;
            }
            else if (testBlock.getBlockData() instanceof Waterlogged){
                Waterlogged wlog = (Waterlogged) testBlock.getBlockData();
                if (!wlog.isWaterlogged()){
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    public static boolean isWaterlogged(Location location){
        if (!(location.getBlock().getBlockData() instanceof Waterlogged)){
            return false;
        }
        Waterlogged wlog = (Waterlogged) location.getBlock().getBlockData();
        return wlog.isWaterlogged();
    }

    public static boolean waterToSides(Location location){
        for (MovecraftLocation shift : SHIFTS){
            if (shift.getY() != 0){
                continue;
            }
            final Block test = location.add(shift.toBukkit(location.getWorld())).getBlock();
            if (test.getType() == Material.WATER){
                return true;
            } else if (test.getBlockData() instanceof Waterlogged){
                final Waterlogged wlog = (Waterlogged) test.getBlockData();
                return wlog.isWaterlogged();
            }
        }
        return false;
    }

    private static final MovecraftLocation[] SHIFTS = {new MovecraftLocation(0,1,0),
            new MovecraftLocation(0, -1, 0),
            new MovecraftLocation(1, 0, 0),
            new MovecraftLocation(-1, 0, 0),
            new MovecraftLocation(0, 0, 1),
            new MovecraftLocation(0, 0, -1)};
}
