package net.countercraft.movecraft.utils;

import net.countercraft.movecraft.MovecraftLocation;
import org.jetbrains.annotations.NotNull;

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
            minZ = startBound.getX();
            maxZ = endBound.getX();
        } else {
            maxZ = startBound.getX();
            minZ = endBound.getX();
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
        return this.getXLength() * this.getYLength() * this.getZLength();
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
}
