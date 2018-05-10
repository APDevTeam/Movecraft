package net.countercraft.movecraft.utils;

import net.countercraft.movecraft.MovecraftLocation;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;

public class HeapHitBox implements Set<MovecraftLocation> {
    private final Set<MovecraftLocation> locationSet = new HashSet<>();
    private final PriorityQueue<Integer> minX = new PriorityQueue<>(),
            maxX = new PriorityQueue<>(10,Collections.reverseOrder()),
            minY = new PriorityQueue<>(),
            maxY = new PriorityQueue<>(10,Collections.reverseOrder()),
            minZ = new PriorityQueue<>(),
            maxZ = new PriorityQueue<>(10,Collections.reverseOrder());

    public HeapHitBox(){
    }

    public HeapHitBox(Collection<MovecraftLocation> collection){
        this.addAll(collection);
    }

    public int getMinX() {
        if(locationSet.isEmpty()){
            throw new EmptyHitBoxException();
        }
        return minX.peek();
    }

    public int getMaxX() {
        if(locationSet.isEmpty()){
            throw new EmptyHitBoxException();
        }
        return maxX.peek();
    }

    public int getMinY() {
        if(locationSet.isEmpty()){
            throw new EmptyHitBoxException();
        }
        return minY.peek();
    }

    public int getMaxY() {
        if(locationSet.isEmpty()){
            throw new EmptyHitBoxException();
        }
        return maxY.peek();
    }

    public int getMinZ() {
        if(locationSet.isEmpty()){
            throw new EmptyHitBoxException();
        }
        return minZ.peek();
    }

    public int getMaxZ() {
        if(locationSet.isEmpty()){
            throw new EmptyHitBoxException();
        }
        return maxZ.peek();
    }

    public int getXLength(){
        if(locationSet.isEmpty()){
            return 0;
        }
        return Math.abs(maxX.peek()-minX.peek());
    }

    public int getYLength(){
        if(locationSet.isEmpty()){
            return 0;
        }
        return maxY.peek()-minY.peek();
    }

    public int getZLength(){
        if(locationSet.isEmpty()){
            throw new EmptyHitBoxException();
        }
        return Math.abs(maxZ.peek()-minZ.peek());
    }

    // Don't Worry About This
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

    // Don't Worry About This
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
        return new MovecraftLocation((minX.peek()+maxX.peek())/2, (minY.peek()+maxY.peek())/2,(minZ.peek()+maxZ.peek())/2);
    }

    public boolean inBounds(MovecraftLocation location){
        if(locationSet.isEmpty()){
            return false;
        }
        return location.getX()> minX.peek() && location.getX() < maxX.peek() &&
                location.getY() > minY.peek() && location.getY() < maxY.peek() &&
                location.getZ() > minZ.peek() && location.getZ() < maxZ.peek();
    }

    public boolean inBounds(double x, double y, double z){
        if(locationSet.isEmpty()){
            return false;
        }
        return x > minX.peek() && x < maxX.peek() &&
                y > minY.peek() && y < maxY.peek() &&
                z > minZ.peek() && z < maxZ.peek();
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
        minX.add(movecraftLocation.getX());
        maxX.add(movecraftLocation.getX());
        minY.add(movecraftLocation.getY());
        maxY.add(movecraftLocation.getY());
        minY.add(movecraftLocation.getZ());
        maxY.add(movecraftLocation.getZ());
        return locationSet.add(movecraftLocation);
    }

    @Override
    public boolean remove(Object o) {
        if(!locationSet.contains(o))
            return false;
        MovecraftLocation remove = (MovecraftLocation)o;
        locationSet.remove(remove);
        minX.remove(((MovecraftLocation) o).getX());
        maxX.remove(((MovecraftLocation) o).getX());
        minY.remove(((MovecraftLocation) o).getY());
        maxY.remove(((MovecraftLocation) o).getY());
        minZ.remove(((MovecraftLocation) o).getZ());
        maxZ.remove(((MovecraftLocation) o).getZ());
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
            }
        }
        return modified;
    }


    @Override
    public void clear() {
        locationSet.clear();
    }

    private class EmptyHitBoxException extends RuntimeException{ }

}
