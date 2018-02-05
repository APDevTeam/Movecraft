package net.countercraft.movecraft.utils;

import net.countercraft.movecraft.MovecraftLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class HitBox implements Set<MovecraftLocation> {
    private final Set<MovecraftLocation> locationSet = new HashSet<>();
    private int minX,maxX,minY,maxY,minZ,maxZ;

    public HitBox(){
    }

    public HitBox(Collection<MovecraftLocation> collection){
        this.addAll(collection);
    }

    public int getMinX() {
        return minX;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public int getXLength(){
        return maxX-minX;
    }

    public int getYLength(){
        return maxY-minY;
    }

    public int getZLegtnh(){
        return maxZ-minZ;
    }

    //TODO: Optomize
    public int getLocalMaxY(int x, int z){
        int yValue=-1;
        for(MovecraftLocation location : locationSet){
            if(location.getX()==x && location.getZ() ==z && location.getY()>yValue){
                yValue=location.getY();
            }
        }
        return yValue;
    }

    public int getLocalMinY(int x, int z){
        int yValue=-1;
        for(MovecraftLocation location : locationSet){
            if(location.getX()==x && location.getZ() ==z && (yValue==-1 || location.getY()>yValue)){
                yValue=location.getY();
            }
        }
        return yValue;
    }

    public MovecraftLocation getMidPoint(){
        return new MovecraftLocation(minX+getXLength()/2, minY+getYLength()/2,minZ+getZLegtnh()/2);
    }

    public boolean inBounds(MovecraftLocation location){
        return location.getX()> minX && location.getX() < maxX &&
                location.getY() > minY && location.getY() < maxY &&
                location.getZ() > minZ && location.getZ() < maxZ;
    }

    public boolean inBounds(double x, double y, double z){
        return x > minX && x < maxX &&
                y > minY && y < maxY &&
                z > minZ && z < maxZ;
    }

    public boolean intersects(HitBox hitBox){
        return hitBox.stream().anyMatch(this::contains);
    }

    @Override
    public int size() {
        return locationSet.size();
    }

    @Override
    public boolean isEmpty() {
        return locationSet.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return locationSet.contains(o);
    }

    public boolean contains(int x, int y, int z){
        return contains(new MovecraftLocation(x,y,z));
    }

    @NotNull
    @Override
    public Iterator<MovecraftLocation> iterator(){
        return new Iterator<MovecraftLocation>() {

            private final Iterator<MovecraftLocation> it = locationSet.iterator();
            private MovecraftLocation last;

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public MovecraftLocation next() {
                return last = it.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
                /*if (last == null) {
                    throw new IllegalStateException();
                }
                it.remove();
                last = null;*/
            }
        };
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return locationSet.toArray();
    }

    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        return locationSet.toArray(a);
    }

    @Override
    public boolean add(MovecraftLocation movecraftLocation) {
        if(movecraftLocation.getX()<minX)
            minX=movecraftLocation.getX();
        if(movecraftLocation.getX()>maxX)
            maxX=movecraftLocation.getX();
        if(movecraftLocation.getY()<minY)
            maxY=movecraftLocation.getY();
        if(movecraftLocation.getY()>maxY)
            maxY=movecraftLocation.getY();
        if(movecraftLocation.getZ()<minZ)
            minZ=movecraftLocation.getZ();
        if(movecraftLocation.getZ()>maxZ)
            maxZ=movecraftLocation.getZ();
        return locationSet.add(movecraftLocation);
    }

    @Override
    public boolean remove(Object o) {
        if(!locationSet.contains(o))
            return false;
        MovecraftLocation remove = (MovecraftLocation)o;
        locationSet.remove(remove);
        if(minX==remove.getX() || maxX == remove.getX() || minY == remove.getY() || maxY==remove.getY() || minZ==remove.getZ() || maxZ==remove.getZ()){
            for (MovecraftLocation location : locationSet){
                if(location.getX()<minX)
                    minX=location.getX();
                if(location.getX()>maxX)
                    maxX=location.getX();
                if(location.getY()<minY)
                    maxY=location.getY();
                if(location.getY()>maxY)
                    maxY=location.getY();
                if(location.getZ()<minZ)
                    minZ=location.getZ();
                if(location.getZ()>maxZ)
                    maxZ=location.getZ();
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return locationSet.containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends MovecraftLocation> c) {
        boolean modified = false;
        for (MovecraftLocation location : c) {
            if (add(location))
                modified = true;
        }
        return modified;
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }


    @Override
    public void clear() {
        locationSet.clear();
        minX=-1;
        maxX=-1;
        minY=-1;
        maxY=-1;
        minZ=-1;
        maxZ=-1;
    }
}
