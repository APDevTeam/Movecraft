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
        return switch (direction.getOppositeFace()) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
            default -> NONE;
        };
    }

    public CruiseDirection getOpposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case EAST -> WEST;
            case WEST -> EAST;
            case UP -> DOWN;
            case DOWN -> UP;
            case NONE -> NONE;
        };
    }

    public CruiseDirection getRotated(MovecraftRotation rotation) {
        return switch(rotation) {
            case CLOCKWISE -> switch (this) {
                case NORTH -> EAST;
                case SOUTH -> WEST;
                case EAST -> SOUTH;
                case WEST -> NORTH;
                default -> this;
            };
            case ANTICLOCKWISE -> getRotated(MovecraftRotation.CLOCKWISE).getOpposite();
            case NONE -> this;
        };
    }
}

