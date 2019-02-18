package net.countercraft.movecraft;

import org.bukkit.block.Block;

public final class RepairLocation implements Comparable<RepairLocation> {
    private final int x;
    private final int y;
    private final int z;
    private final Block block;
    public RepairLocation(int x, int y, int z, Block block){
        this.x = x;
        this.y = y;
        this.z = z;
        this.block = block;
    }
    @Override
    public int compareTo(RepairLocation other) {
        return 0;
    }
}
