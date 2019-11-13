package net.countercraft.movecraft.mapUpdater.update;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.utils.HashHitBox;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.util.Vector;

public class WaterlogUpdateCommand extends UpdateCommand{
    private final Craft craft;
    private final Vector[] SHIFTS = {new Vector(0,1,0),
    new Vector(0, -1, 0),
    new Vector(1, 0, 0),
    new Vector(-1, 0, 0),
            new Vector(0, 0, 1),
            new Vector(0, 0, -1)};
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
            Waterlogged wLog = (Waterlogged) b.getBlockData();
            if (b.getY() > craft.getWaterLine()){
                wLog.setWaterlogged(false);
                b.setBlockData(wLog);
                continue;
            }
            if (wLog instanceof TrapDoor){
                TrapDoor tDoor = (TrapDoor) wLog;
                if (tDoor.getHalf() == Bisected.Half.BOTTOM && b.getRelative(0,1,0).getType() == Material.WATER){
                    tDoor.setWaterlogged(true);

                }
                if (tDoor.getHalf() == Bisected.Half.TOP && b.getRelative(0,-1,0).getType() == Material.WATER){
                    tDoor.setWaterlogged(true);
                }
                if (b.getRelative(tDoor.getFacing().getOppositeFace()).getType() == Material.WATER){
                    tDoor.setWaterlogged(true);
                }
                b.setBlockData(tDoor);
            } else if (wLog instanceof Slab){
                Slab slab = (Slab) wLog;
                if (slab.getType() == Slab.Type.TOP){
                    for (Vector shift : SHIFTS){
                        final Location testLoc = b.getLocation().add(shift);
                        if (shift.getY() == 1){
                            continue;
                        }
                        if (!(testLoc.getBlock().getBlockData() instanceof Waterlogged)) {
                            continue;
                        }
                        slab.setWaterlogged(true);
                        b.setBlockData(slab);
                        break;
                    }
                }
            }
            else {
                if (b.getRelative(0,1,0).getType() == Material.WATER ||
                        b.getRelative(0,-1,0).getType() == Material.WATER ||
                        b.getRelative(0,0,1).getType() == Material.WATER ||
                        b.getRelative(0,0,-1).getType() == Material.WATER ||
                        b.getRelative(1,0,0).getType() == Material.WATER ||
                        b.getRelative(-1,0,0).getType() == Material.WATER ){
                    wLog.setWaterlogged(true);
                    b.setBlockData(wLog);
                }
            }
        }
    }
}
