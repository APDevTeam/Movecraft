package net.countercraft.movecraft.mapUpdater.update;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.utils.HashHitBox;
import net.countercraft.movecraft.utils.HitBox;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.scheduler.BukkitRunnable;

public class WaterlogUpdateCommand extends UpdateCommand{
    private final Craft craft;
    public WaterlogUpdateCommand(Craft craft){
        this.craft = craft;
    }
    @Override
    public void doUpdate() {
        HashHitBox hitBox = craft.getHitBox();
        for (MovecraftLocation moveLoc : hitBox){
            Block b = moveLoc.toBukkit(craft.getW()).getBlock();
            if (!(b.getBlockData() instanceof Waterlogged)){
                continue;
            }
            BukkitRunnable runnable = null;
            Waterlogged wLog = (Waterlogged) b.getBlockData();
            if (b.getY() > craft.getWaterLine()){
                runnable = new BukkitRunnable() {
                    @Override
                    public void run() {
                        wLog.setWaterlogged(false);
                    }
                };
                runnable.runTask(Movecraft.getInstance());
                continue;
            }
            if (wLog instanceof TrapDoor){
                TrapDoor tDoor = (TrapDoor) wLog;
                if (tDoor.getHalf() == Bisected.Half.BOTTOM && b.getRelative(0,1,0).getType() == Material.WATER){
                    runnable = new BukkitRunnable() {
                        @Override
                        public void run() {
                            tDoor.setWaterlogged(true);
                        }
                    };

                }
                if (tDoor.getHalf() == Bisected.Half.TOP && b.getRelative(0,-1,0).getType() == Material.WATER){
                    runnable = new BukkitRunnable() {
                        @Override
                        public void run() {
                            tDoor.setWaterlogged(true);
                        }
                    };
                }
                if (b.getRelative(tDoor.getFacing().getOppositeFace()).getType() == Material.WATER){
                    runnable = new BukkitRunnable() {
                        @Override
                        public void run() {
                            tDoor.setWaterlogged(true);
                        }
                    };
                }
            } else {
                if (b.getRelative(0,1,0).getType() == Material.WATER ||
                        b.getRelative(0,-1,0).getType() == Material.WATER ||
                        b.getRelative(0,0,1).getType() == Material.WATER ||
                        b.getRelative(0,0,-1).getType() == Material.WATER ||
                        b.getRelative(1,0,0).getType() == Material.WATER ||
                        b.getRelative(-1,0,0).getType() == Material.WATER ){
                    runnable = new BukkitRunnable() {
                        @Override
                        public void run() {
                            wLog.setWaterlogged(true);
                        }
                    };

                }
            }
            if (runnable == null)
                return;
            runnable.runTask(Movecraft.getInstance());
        }
    }
}
