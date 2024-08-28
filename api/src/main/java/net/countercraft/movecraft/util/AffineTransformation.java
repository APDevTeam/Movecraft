package net.countercraft.movecraft.util;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.MovecraftRotation;
import org.bukkit.block.structure.Mirror;
import org.jetbrains.annotations.NotNull;

// TODO: Implement with 4d matrix
public record AffineTransformation(){
    public static @NotNull AffineTransformation NONE = null;
    public static @NotNull AffineTransformation of(MovecraftLocation translation){ return null; }
    public static @NotNull AffineTransformation of(MovecraftRotation rotation){ return null; }
    public @NotNull AffineTransformation mult(AffineTransformation other){ return null; }
    public @NotNull MovecraftLocation apply(MovecraftLocation location){ return location; }
    public @NotNull MovecraftRotation extractRotation(){ return null; }
    public @NotNull Mirror extractMirror(){ return null; }
}
