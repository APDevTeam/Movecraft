package net.countercraft.movecraft;

import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public enum CruiseDirection {
    NORTH,
    SOUTH,
    EAST,
    WEST,
    UP,
    DOWN,
    NONE;

    @Contract(pure = true)
    public static CruiseDirection fromBlockFace(@NotNull BlockFace direction) {
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

    public CruiseDirection getOpposite2D() {
        return switch (this) {
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case EAST -> WEST;
            case WEST -> EAST;
            default -> this;
        };
    }

    public CruiseDirection getRotated2D(@NotNull MovecraftRotation rotation) {
        return switch(rotation) {
            case CLOCKWISE -> switch (this) {
                case NORTH -> EAST;
                case SOUTH -> WEST;
                case EAST -> SOUTH;
                case WEST -> NORTH;
                default -> this;
            };
            case ANTICLOCKWISE -> getRotated2D(MovecraftRotation.CLOCKWISE).getOpposite2D();
            case NONE -> this;
        };
    }
}

