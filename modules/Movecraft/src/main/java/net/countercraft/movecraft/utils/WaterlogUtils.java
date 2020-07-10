package net.countercraft.movecraft.utils;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.TrapDoor;

public class WaterlogUtils {
    private static Pair<Material, Object> DEFAULT_PHASE_BLOCK = new Pair<>(Material.AIR, 0);
    private WaterlogUtils(){

    }
    public static void waterlogBlocksOnCraft(Craft craft, HitBox interior) {

        for (MovecraftLocation ml : craft.getHitBox()) {
            final Material phaseBlock = craft.getPhaseBlocks().getOrDefault(ml.toBukkit(craft.getWorld()), DEFAULT_PHASE_BLOCK).getLeft();
            boolean waterlog = phaseBlock == Material.WATER;
            Block b = ml.toBukkit(craft.getWorld()).getBlock();
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

            if (!exteriorBlock) {
                waterlog = false;
            }
            if (wLog instanceof TrapDoor) {
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
            } else if (wLog instanceof Stairs) {
                Stairs s = (Stairs) wLog;
                MovecraftLocation facingLoc = MathUtils.bukkit2MovecraftLoc(b.getRelative(s.getFacing().getOppositeFace()).getLocation());

                if (s.getHalf() == Bisected.Half.TOP && (interior.contains(ml.translate(0, -1, 0)) || craft.getHitBox().contains(ml.translate(0, -1, 0)))) {
                    waterlog = false;
                } else if (s.getHalf() == Bisected.Half.BOTTOM && (interior.contains(ml.translate(0, 1, 0)) || craft.getHitBox().contains(ml.translate(0, 1, 0)))) {
                    waterlog = false;
                }
                if (interior.contains(facingLoc) || craft.getHitBox().contains(facingLoc)) {
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
