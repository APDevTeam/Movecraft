package net.countercraft.movecraft.util;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.MovecraftRotation;
import org.bukkit.block.structure.Mirror;
import org.ejml.simple.SimpleMatrix;
import org.jetbrains.annotations.NotNull;

// TODO: Implement with 4d matrix
public record AffineTransformation(SimpleMatrix backingMatrix){
    public static @NotNull AffineTransformation NONE = new AffineTransformation(SimpleMatrix.identity(4));
    public static @NotNull AffineTransformation of(MovecraftLocation translation){
        var ret = SimpleMatrix.identity(4);
        ret.set(3, 0, translation.getX());
        ret.set(3, 1, translation.getY());
        ret.set(3, 2, translation.getZ());

        return new AffineTransformation(ret);
    }
    public static @NotNull AffineTransformation of(MovecraftRotation rotation){

        return null;
    }
    public @NotNull AffineTransformation mult(AffineTransformation other){
        return new AffineTransformation(backingMatrix.mult(other.backingMatrix));
    }

    public @NotNull MovecraftLocation apply(MovecraftLocation location){
        var transformed = backingMatrix.mult(new SimpleMatrix(new double[]{location.getX(), location.getY(), location.getZ(), 1}));

        return new MovecraftLocation((int) transformed.get(0), (int) transformed.get(1), (int) transformed.get(2));
    }
    public @NotNull MovecraftRotation extractRotation(){ return null; }
    public @NotNull Mirror extractMirror(){ return null; }
}
