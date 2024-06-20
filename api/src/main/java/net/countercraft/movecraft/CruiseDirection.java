package net.countercraft.movecraft;

import org.bukkit.block.BlockFace;

public enum CruiseDirection {
    NORTH((byte) 0x3), //0x3
    SOUTH((byte) 0x2), //0x2
    EAST((byte) 0x4), //0x4
    WEST((byte) 0x5), //0x5
    UP((byte) 0x42), //0x42
    DOWN((byte) 0x43), //0x43
    NONE((byte) 0x0);

    private final byte raw;

    CruiseDirection(byte rawDirection) {
        raw = rawDirection;
    }

    public byte getRaw() {
        return raw;
    }
    public static CruiseDirection fromRaw(byte rawDirection) {
        if(rawDirection == (byte) 0x3)
            return NORTH;
        else if(rawDirection == (byte) 0x2)
            return SOUTH;
        else if(rawDirection == (byte) 0x4)
            return EAST;
        else if(rawDirection == (byte) 0x5)
            return WEST;
        else if(rawDirection == (byte) 0x42)
            return UP;
        else if(rawDirection == (byte) 0x43)
            return DOWN;
        else
            return NONE;
    }

    public static CruiseDirection fromBlockFace(BlockFace direction) {
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

