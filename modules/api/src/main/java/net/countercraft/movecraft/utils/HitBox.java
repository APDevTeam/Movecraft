package net.countercraft.movecraft.utils;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.exception.EmptyHitBoxException;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public interface HitBox extends Iterable<MovecraftLocation>{

    public int getMinX();
    public int getMinY();
    public int getMinZ();
    public int getMaxX();
    public int getMaxY();
    public int getMaxZ();

    default public int getXLength(){
        if(this.isEmpty()){
            return 0;
        }
        return Math.abs(this.getMaxX()-this.getMinX());
    }
    default public int getYLength(){
        if(this.isEmpty()){
            return 0;
        }
        return Math.abs(this.getMaxY()-this.getMinY());
    }
    default public int getZLength(){
        if(this.isEmpty()){
            return 0;
        }
        return Math.abs(this.getMaxZ()-this.getMinZ());
    }

    default public boolean isEmpty(){
        return this.size() == 0;
    }
    public int size();

    @NotNull
    default public MovecraftLocation getMidPoint(){
        if(this.isEmpty()){
            throw new EmptyHitBoxException();
        }
        return new MovecraftLocation((this.getMinX()+this.getMaxX())/2, (this.getMinY()+this.getMaxY())/2,(this.getMinZ()+this.getMaxZ())/2);
    }

    @NotNull
    @Override
    public Iterator<MovecraftLocation> iterator();

    public boolean contains(@NotNull MovecraftLocation location);

    default boolean contains(int x, int y, int z){
        return this.contains(new MovecraftLocation(x,y,z));
    }

    boolean containsAll(Collection<? extends MovecraftLocation> collection);

    default boolean inBounds(double x, double y, double z){
        if(this.isEmpty()){
            return false;
        }
        return x >= this.getMinX() && x <= this.getMaxX() &&
                y >= this.getMinY() && y <= this.getMaxY()&&
                z >= this.getMinZ() && z <= this.getMaxZ();
    }

    default boolean inBounds(MovecraftLocation location){
        return this.inBounds(location.getX(),location.getY(),location.getZ());
    }

    @NotNull
    default SolidHitBox boundingHitBox(){
        return new SolidHitBox(new MovecraftLocation(this.getMinX(),this.getMinY(),this.getMinZ()),
                new MovecraftLocation(this.getMaxX(),this.getMaxY(),this.getMaxZ()));
    }

    @NotNull
    default Set<MovecraftLocation> asSet(){
        Set<MovecraftLocation> output = new HashSet<>();
        for(MovecraftLocation location : this){
            output.add(location);
        }
        return Collections.unmodifiableSet(output);
    }
}

