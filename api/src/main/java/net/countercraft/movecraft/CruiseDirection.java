package net.countercraft.movecraft;

import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class CruiseDirection extends Vector {

    public static CruiseDirection NORTH = new CruiseDirection(0,0,-1);
    public static CruiseDirection SOUTH = new CruiseDirection(0,0,1);
    public static CruiseDirection EAST = new CruiseDirection(1,0,0);
    public static CruiseDirection WEST = new CruiseDirection(-1,0,0);
    public static CruiseDirection UP = new CruiseDirection(0,1,0);
    public static CruiseDirection DOWN = new CruiseDirection(0,-1,0);
    public static CruiseDirection NONE = new CruiseDirection(0,0,0);

    public CruiseDirection(final Vector direction) {
        this(direction.getX(), direction.getY(), direction.getZ());
    }

    public CruiseDirection(double x, double y, double z) {
        super(x, y, z);
        if (!this.isZero())
            this.normalize();
    }

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
        return new CruiseDirection(-this.getX(), this.getY(), -this.getZ());
    }

    public CruiseDirection getRotated2D(@NotNull MovecraftRotation rotation) {
        return switch(rotation) {
            case CLOCKWISE -> new CruiseDirection(-this.getZ(), this.getY(), this.getX());
            case ANTICLOCKWISE -> getRotated2D(MovecraftRotation.CLOCKWISE).getOpposite2D();
            case NONE -> this;
        };
    }
}

