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
        if(locationSet.isEmpty()){
            throw new EmptyHitBoxException();
        }
        return minX;
    }

    public int getMaxX() {
        if(locationSet.isEmpty()){
            throw new EmptyHitBoxException();
        }
        return maxX;
    }

    public int getMinY() {
        if(locationSet.isEmpty()){
            throw new EmptyHitBoxException();
        }
        return minY;
    }

    public int getMaxY() {
        if(locationSet.isEmpty()){
            throw new EmptyHitBoxException();
        }
        return maxY;
    }

    public int getMinZ() {
        if(locationSet.isEmpty()){
            throw new EmptyHitBoxException();
        }
        return minZ;
    }

    public int getMaxZ() {
        if(locationSet.isEmpty()){
            throw new EmptyHitBoxException();
        }
        return maxZ;
    }

    public int getXLength(){
        if(locationSet.isEmpty()){
            return 0;
        }
        return Math.abs(maxX-minX);
    }

    public int getYLength(){
        if(locationSet.isEmpty()){
            return 0;
        }
        return maxY-minY;
    }

    public int getZLength(){
        if(locationSet.isEmpty()){
            throw new EmptyHitBoxException();
        }
        return Math.abs(maxZ-minZ);
    }

    //TODO: Optomize
    public int getLocalMaxY(int x, int z){
        if(locationSet.isEmpty()){
            throw new EmptyHitBoxException();
        }
        int yValue=-1;
        for(MovecraftLocation location : locationSet){
            if(location.getX()==x && location.getZ() ==z && location.getY()>yValue){
                yValue=location.getY();
            }
        }
        return yValue;
    }

    public int getLocalMinY(int x, int z){
        if(locationSet.isEmpty()){
            throw new EmptyHitBoxException();
        }
        int yValue=-1;
        for(MovecraftLocation location : locationSet){
            if(location.getX()==x && location.getZ() ==z && (yValue==-1 || location.getY()>yValue)){
                yValue=location.getY();
            }
        }
        return yValue;
    }

    public MovecraftLocation getMidPoint(){
        if(locationSet.isEmpty()){
            throw new EmptyHitBoxException();
        }
        return new MovecraftLocation((minX+maxX)/2, (minY+maxY)/2,(minZ+maxZ)/2);
    }

    public boolean inBounds(MovecraftLocation location){
        if(locationSet.isEmpty()){
            return false;
        }
        return location.getX()> minX && location.getX() < maxX &&
                location.getY() > minY && location.getY() < maxY &&
                location.getZ() > minZ && location.getZ() < maxZ;
    }

    public boolean inBounds(double x, double y, double z){
        if(locationSet.isEmpty()){
            return false;
        }
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
        if(locationSet.isEmpty() || movecraftLocation.getX() < minX)
            minX=movecraftLocation.getX();
        if(locationSet.isEmpty() || movecraftLocation.getX() > maxX)
            maxX=movecraftLocation.getX();
        if(locationSet.isEmpty() || movecraftLocation.getY() < minY)
            minY=movecraftLocation.getY();
        if(locationSet.isEmpty() || movecraftLocation.getY() > maxY)
            maxY=movecraftLocation.getY();
        if(locationSet.isEmpty() || movecraftLocation.getZ() < minZ)
            minZ=movecraftLocation.getZ();
        if(locationSet.isEmpty() || movecraftLocation.getZ() > maxZ)
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
            updateBounds();
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
        boolean updateBounds = false;
        boolean modified = false;
        for(Object o : c){
            if(locationSet.remove(o)) {
                modified = true;
                MovecraftLocation location = (MovecraftLocation) o;
                if (location.getX() < minX)
                    updateBounds=true;
                if (location.getX() > maxX)
                    updateBounds=true;
                if (location.getY() < minY)
                    updateBounds=true;
                if (location.getY() > maxY)
                    updateBounds=true;
                if (location.getZ() < minZ)
                    updateBounds=true;
                if (location.getZ() > maxZ)
                    updateBounds=true;
            }
        }
        if(updateBounds){
            updateBounds();
        }
        return modified;
    }


    @Override
    public void clear() {
        locationSet.clear();
    }

    private class EmptyHitBoxException extends RuntimeException{ }

    private void updateBounds(){
        for (MovecraftLocation location : locationSet){
            if(location.getX()<minX)
                minX=location.getX();
            if(location.getX()>maxX)
                maxX=location.getX();
            if(location.getY()<minY)
                minY=location.getY();
            if(location.getY()>maxY)
                maxY=location.getY();
            if(location.getZ()<minZ)
                minZ=location.getZ();
            if(location.getZ()>maxZ)
                maxZ=location.getZ();
        }
    }
}
