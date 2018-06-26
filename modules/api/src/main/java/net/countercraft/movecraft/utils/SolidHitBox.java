package net.countercraft.movecraft.utils;

import net.countercraft.movecraft.MovecraftLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;

final public class SolidHitBox implements HitBox{
    final private int minX, minY, minZ, maxX, maxY, maxZ;


    public SolidHitBox(MovecraftLocation startBound, MovecraftLocation endBound){
        if(startBound.getX() < endBound.getX()){
            minX = startBound.getX();
            maxX = endBound.getX();
        } else {
            maxX = startBound.getX();
            minX = endBound.getX();
        }
        if(startBound.getY() < endBound.getY()){
            minY = startBound.getY();
            maxY = endBound.getY();
        } else {
            maxY = startBound.getY();
            minY = endBound.getY();
        }
        if(startBound.getZ() < endBound.getZ()){
            minZ = startBound.getZ();
            maxZ = endBound.getZ();
        } else {
            maxZ = startBound.getZ();
            minZ = endBound.getZ();
        }
    }

    @Override
    public int getMinX() {
        return minX;
    }

    @Override
    public int getMinY() {
        return minY;
    }

    @Override
    public int getMinZ() {
        return minZ;
    }

    @Override
    public int getMaxX() {
        return maxX;
    }

    @Override
    public int getMaxY() {
        return maxY;
    }

    @Override
    public int getMaxZ() {
        return maxZ;
    }

    @Override
    public int size() {
        return (this.getXLength() + 1) * (this.getYLength() + 1) * (this.getZLength() + 1);
    }

    @Override
    public boolean isEmpty(){
        //can never be empty
        return false;
    }

    @Override
    public int getXLength(){
        return Math.abs(this.getMaxX()-this.getMinX());
    }
    @Override
    public int getYLength(){
        return Math.abs(this.getMaxY()-this.getMinY());
    }
    @Override
    public int getZLength(){
        return Math.abs(this.getMaxZ()-this.getMinZ());
    }

    @NotNull
    @Override
    public Iterator<MovecraftLocation> iterator() {
        return new Iterator<MovecraftLocation>() {
            private int lastX = minX;
            private int lastY = minY;
            private int lastZ = minZ;
            @Override
            public boolean hasNext() {
                return lastZ <= maxZ;
            }

            @Override
            public MovecraftLocation next() {
                MovecraftLocation output = new MovecraftLocation(lastX,lastY,lastZ);
                lastX++;
                if (lastX > maxX){
                    lastX = minX;
                    lastY++;
                }
                if(lastY > maxY){
                    lastY = minY;
                    lastZ++;
                }
                return output;
            }
        };
    }

    @Override
    public boolean contains(@NotNull MovecraftLocation location) {
        return location.getX() >= minX &&
                location.getX() <= maxX &&
                location.getY() >= minY &&
                location.getY() <= maxY &&
                location.getZ() >= minZ &&
                location.getZ() <= maxZ;
    }

    @Override
    public boolean containsAll(Collection<? extends MovecraftLocation> collection) {
        for (MovecraftLocation location : collection){
            if (!this.contains(location)){
                return false;
            }
        }
        return true;
    }

    @Nullable
    public SolidHitBox subtract(SolidHitBox other){
        if(this.minX > other.maxX || this.maxX < other.minX || this.minY > other.minY || this.maxY < other.minY || this.minZ > other.maxZ || this.maxZ < other.minZ)
            return null;

        return new SolidHitBox(
                new MovecraftLocation(
                        Math.max(other.getMinX(), this.getMinX()),
                        Math.max(other.getMinY(), this.getMinY()),
                        Math.max(other.getMinZ(),this.getMinZ())),
                new MovecraftLocation(
                        Math.min(other.getMaxX(), this.getMaxX()),
                        Math.min(other.getMaxY(), this.getMaxY()),
                        Math.min(other.getMaxZ(), this.getMaxZ()))
                );
    }
}
