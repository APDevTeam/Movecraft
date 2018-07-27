package net.countercraft.movecraft.utils;

import net.countercraft.movecraft.MovecraftLocation;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class HeapHitBox implements MutableHitBox {
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
        return Math.abs(this.getMaxX()-this.getMinX());
    }

    public int getYLength(){
        if(locationSet.isEmpty()){
            return 0;
        }
        return Math.abs(this.getMaxY()-this.getMinY());
    }

    public int getZLength(){
        if(locationSet.isEmpty()){
            throw new EmptyHitBoxException();
        }
        return Math.abs(this.getMaxZ()-this.getMinZ());
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

    @NotNull
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
        return location.getX()>= minX.peek() && location.getX() <= maxX.peek() &&
                location.getY() >= minY.peek() && location.getY() <= maxY.peek() &&
                location.getZ() >= minZ.peek() && location.getZ() <= maxZ.peek();
    }

    public boolean inBounds(double x, double y, double z){
        if(locationSet.isEmpty()){
            return false;
        }
        return x >= minX.peek() && x <= maxX.peek() &&
                y >= minY.peek() && y <= maxY.peek() &&
                z >= minZ.peek() && z <= maxZ.peek();
    }

    public boolean intersects(HashHitBox hitBox){
        for(MovecraftLocation location : hitBox){
            if(this.contains(location)){
                return true;
            }
        }
        return false;
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
    public boolean contains(@NotNull MovecraftLocation location) {
        return locationSet.contains(location);
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

    @Override
    public boolean add(@NotNull MovecraftLocation movecraftLocation) {
        minX.add(movecraftLocation.getX());
        maxX.add(movecraftLocation.getX());
        minY.add(movecraftLocation.getY());
        maxY.add(movecraftLocation.getY());
        minY.add(movecraftLocation.getZ());
        maxY.add(movecraftLocation.getZ());
        return locationSet.add(movecraftLocation);
    }

    @Override
    public boolean remove(@NotNull MovecraftLocation location) {
        if(!locationSet.contains(location))
            return false;
        locationSet.remove(location);
        minX.remove(location.getX());
        maxX.remove(location.getX());
        minY.remove(location.getY());
        maxY.remove(location.getY());
        minZ.remove(location.getZ());
        maxZ.remove(location.getZ());
        return true;
    }

    @Override
    public boolean containsAll(@NotNull Collection<? extends MovecraftLocation> c) {
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
    public boolean addAll(@NotNull HitBox hitBox) {
        boolean modified = false;
        for (MovecraftLocation location : hitBox) {
            if (add(location))
                modified = true;
        }
        return modified;
    }

    @Override
    public boolean removeAll(@NotNull Collection<? extends MovecraftLocation> c) {
        boolean updateBounds = false;
        boolean modified = false;
        for(MovecraftLocation location : c){
            if(locationSet.remove(location)) {
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean removeAll(@NotNull HitBox hitBox) {
        boolean updateBounds = false;
        boolean modified = false;
        for(MovecraftLocation location : hitBox){
            if(locationSet.remove(location)) {
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
