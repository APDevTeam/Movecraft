package net.countercraft.movecraft;

import org.bukkit.block.BlockFace;

public enum CruiseDirection {
    NORTH, //0x3
    SOUTH, //0x2
    EAST, //0x4
    WEST, //0x5
    UP, //0x42
    DOWN, //0x43
    NONE;

    public static CruiseDirection fromBlockFace(BlockFace direction)
    {
        if(direction.getOppositeFace() == BlockFace.NORTH)
            return NORTH;
        else if(direction.getOppositeFace() == BlockFace.SOUTH)
            return SOUTH;
        else if(direction.getOppositeFace() == BlockFace.EAST)
            return EAST;
        else if(direction.getOppositeFace() == BlockFace.WEST)
            return WEST;
        else if(direction.getOppositeFace() == BlockFace.UP)
            return UP;
        else if(direction.getOppositeFace() == BlockFace.DOWN)
            return DOWN;
        else
            return NONE;
    }
}

