package net.countercraft.movecraft;

import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

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

    public static CruiseDirection fromString(String input) {
        final String sanitized = input.toLowerCase(Locale.ROOT);

        return switch (sanitized) {
            case "north", "n" -> CruiseDirection.NORTH;
            case "south", "s" -> CruiseDirection.SOUTH;
            case "east", "e" -> CruiseDirection.EAST;
            case "west", "w" -> CruiseDirection.WEST;
            case "up", "u" -> CruiseDirection.UP;
            case "down", "d" -> CruiseDirection.DOWN;
            default -> CruiseDirection.NONE;
        };
    }

    public static List<String> valuesString() {
        return Arrays.stream(values()).map(v -> v.toString().toLowerCase(Locale.ROOT)).toList();
    }
}

