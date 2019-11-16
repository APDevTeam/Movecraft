package net.countercraft.movecraft.mapUpdater.update;

import org.bukkit.Location;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.util.Vector;

public class WaterlogUpdateCommand extends UpdateCommand{
    private final Location location;
    private final boolean waterlogged;

    public WaterlogUpdateCommand(Location location, boolean waterlogged){
        this.location = location;

        this.waterlogged = waterlogged;
    }

    @Override
    public void doUpdate() {
            final Waterlogged wLog = (Waterlogged) location.getBlock().getBlockData();
            wLog.setWaterlogged(waterlogged);
            location.getBlock().setBlockData(wLog);
    }


    private final Vector[] SHIFTS = {new Vector(0,1,0),
            new Vector(0, -1, 0),
            new Vector(1, 0, 0),
            new Vector(-1, 0, 0),
            new Vector(0, 0, 1),
            new Vector(0, 0, -1)};
}
