package net.countercraft.movecraft;

import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** This class is mutable! */
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

    @Override
    public @NotNull CruiseDirection clone() {
        return new CruiseDirection(this.getX(), this.getY(), this.getZ());
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

    // Maybe switch to rotate2D(double)
    public CruiseDirection getRotated2D(@NotNull MovecraftRotation rotation) {
        return switch(rotation) {
            case CLOCKWISE -> new CruiseDirection(-this.getZ(), this.getY(), this.getX());
            case ANTICLOCKWISE -> getRotated2D(MovecraftRotation.CLOCKWISE).getOpposite2D();
            case NONE -> this;
        };
    }

    public boolean isVertical() {
        return this.getX() == 0.0 && this.getZ() == 0.0;
    }

    /** Angle in radians, rotates anticlockwise. */
    public void rotate2D(double angle) {
        this.rotateAroundY(angle);
    }

    static final double PI_HALF = Math.PI / 2;

    /** Rise or dive (if angle is negative), angle in radians. Will default to UP (or DOWN) if risen too much. */
    public void rise2D(double angle) {
        Vector perpendicular = new Vector(this.getX(), 0, this.getZ()).rotateAroundY(PI_HALF);
        if (angle > 0) {
            angle = Math.min(angle, this.angle(UP));
        } else {
            angle = Math.max(angle, -this.angle(DOWN));
        }
        this.rotateAroundNonUnitAxis(perpendicular, angle);
    }

    public double getYaw() {
        // Adjusted, so the values match whatever is present in the crosshair
        return Math.atan2(-this.x, this.z);
    }

    public double getYawInDegree() {
        final double yawRadian = this.getYaw();
        final double yawDegree = Math.toDegrees(yawRadian);
        return (yawDegree + 360) % 360;
    }

}

