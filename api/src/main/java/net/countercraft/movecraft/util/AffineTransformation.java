package net.countercraft.movecraft.util;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.MovecraftRotation;
import org.bukkit.block.structure.Mirror;
import org.ejml.simple.SimpleMatrix;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class AffineTransformation {
    public static @NotNull AffineTransformation UNIT = new AffineTransformation(SimpleMatrix.identity(4), MovecraftRotation.NONE);
    private final SimpleMatrix backingMatrix;
    private final MovecraftRotation rotation;

    private AffineTransformation(SimpleMatrix backingMatrix, MovecraftRotation rotation) {
        this.backingMatrix = backingMatrix;
        this.rotation = rotation;
    }

    public static @NotNull AffineTransformation of(MovecraftLocation translation) {
        var ret = SimpleMatrix.identity(4);
        ret.set(3, 0, translation.getX());
        ret.set(3, 1, translation.getY());
        ret.set(3, 2, translation.getZ());

        return new AffineTransformation(ret, MovecraftRotation.NONE);
    }

    public static @NotNull AffineTransformation of(MovecraftRotation rotation) {
        var ret = SimpleMatrix.identity(4);
        switch (rotation) {
            case NONE:
                break;
            case CLOCKWISE:
                ret.set(0, 0, 0);
                ret.set(1, 1, 0);
                ret.set(0, 1, -1);
                ret.set(1, 0, 1);
                break;
            case ANTICLOCKWISE:
                ret.set(0, 0, 0);
                ret.set(1, 1, 0);
                ret.set(0, 1, 1);
                ret.set(1, 0, -1);
                break;
        }

        return new AffineTransformation(ret, rotation);
    }

    public @NotNull AffineTransformation mult(AffineTransformation other) {
        // Currently, MovecraftRotation does not support 180 degree rotations
        // To work around this, we simply prefer the other transformations rotation
        // TODO: Implement 180 degree MovecraftRotation

        return new AffineTransformation(backingMatrix.mult(other.backingMatrix), other.rotation == MovecraftRotation.NONE ? rotation : other.rotation);
    }

    public @NotNull MovecraftLocation apply(MovecraftLocation location) {
        var transformed = backingMatrix.mult(new SimpleMatrix(new double[]{location.getX(), location.getY(), location.getZ(), 1}));

        return new MovecraftLocation((int) transformed.get(0), (int) transformed.get(1), (int) transformed.get(2));
    }

    public @NotNull MovecraftRotation extractRotation() {
        return rotation;
    }

    public @NotNull Mirror extractMirror() {
        return Mirror.NONE;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (AffineTransformation) obj;
        return Objects.equals(this.backingMatrix, that.backingMatrix) &&
            Objects.equals(this.rotation, that.rotation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(backingMatrix, rotation);
    }

    @Override
    public String toString() {
        return "AffineTransformation[" +
            "backingMatrix=" + backingMatrix + ", " +
            "rotation=" + rotation + ']';
    }

}
