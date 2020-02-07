package net.countercraft.movecraft.utils;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.TrapDoor;

public class WaterlogUtils {
    private WaterlogUtils(){

    }
    public static void waterlogBlocksOnCraft(Craft craft, HitBox interior) {

        for (MovecraftLocation ml : craft.getHitBox()) {
            boolean waterlog = true;
            Block b = ml.toBukkit(craft.getW()).getBlock();
            if (!(b.getBlockData() instanceof Waterlogged)) {
                continue;
            }
            Waterlogged wLog = (Waterlogged) b.getBlockData();
            boolean exteriorBlock = false;
            for (MovecraftLocation shift : SHIFTS) {
                if (craft.getHitBox().contains(ml.subtract(shift)) || interior.contains(ml.subtract(shift))) {
                    continue;
                }
                exteriorBlock = true;
                break;
            }
            if (!exteriorBlock || ml.getY() > craft.getWaterLine() || !adjacentToWater(ml.toBukkit(craft.getW()))) {
                waterlog = false;
            } else if (wLog instanceof TrapDoor) {
                TrapDoor td = (TrapDoor) wLog;
                if (td.getHalf() == Bisected.Half.TOP && (interior.contains(ml.translate(0, -1, 0)) || craft.getHitBox().contains(ml.translate(0, -1, 0)))) {
                    waterlog = false;
                } else if (td.getHalf() == Bisected.Half.BOTTOM && (interior.contains(ml.translate(0, 1, 0)) || craft.getHitBox().contains(ml.translate(0, 1, 0)))) {
                    waterlog = false;
                }
            } else if (wLog instanceof Slab) {
                Slab s = (Slab) wLog;
                if (s.getType() == Slab.Type.TOP && (interior.contains(ml.translate(0, -1, 0)) || craft.getHitBox().contains(ml.translate(0, -1, 0)))) {
                    waterlog = false;
                } else if (s.getType() == Slab.Type.BOTTOM && (interior.contains(ml.translate(0, 1, 0)) || craft.getHitBox().contains(ml.translate(0, 1, 0)))) {
                    waterlog = false;
                }
            }
            wLog.setWaterlogged(waterlog);
            b.setBlockData(wLog);
        }
    }

    private static boolean isWaterlogged(Location location){
        if (!(location.getBlock().getBlockData() instanceof Waterlogged)){
            return false;
        }
        Waterlogged wlog = (Waterlogged) location.getBlock().getBlockData();
        return wlog.isWaterlogged();
    }

    private static boolean adjacentToWater(Location location){
        for (MovecraftLocation shift : SHIFTS){
            final Block test = location.add(shift.toBukkit(location.getWorld())).getBlock();
            if (test.getType() == Material.WATER){
                return true;
            } else if (test.getBlockData() instanceof Waterlogged){
                final Waterlogged wlog = (Waterlogged) test.getBlockData();
                if (!wlog.isWaterlogged()) {
                    continue;
                }
                return true;
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
