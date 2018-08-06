package net.countercraft.movecraft.utils;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.exception.EmptyHitBoxException;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class HashHitBox implements MutableHitBox {
    private final Set<MovecraftLocation> locationSet = new HashSet<>();
    private int minX,maxX,minY,maxY,minZ,maxZ;

    public HashHitBox(){
    }

    public HashHitBox(Collection<? extends MovecraftLocation> collection){
        this.addAll(collection);
    }
    public HashHitBox(HitBox hitBox){
        this.addAll(hitBox);
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

    @NotNull
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
        return location.getX() >= minX && location.getX() <= maxX &&
                location.getY() >= minY && location.getY() <= maxY &&
                location.getZ() >= minZ && location.getZ() <= maxZ;
    }

    public boolean inBounds(double x, double y, double z){
        if(locationSet.isEmpty()){
            return false;
        }
        return x >= minX && x <= maxX &&
                y >= minY && y <= maxY &&
                z >= minZ && z <= maxZ;
    }

    public boolean intersects(HitBox hitBox){
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
    public boolean remove(@NotNull MovecraftLocation location) {
        if(!locationSet.contains(location))
            return false;
        locationSet.remove(location);
        if(minX==location.getX() || maxX == location.getX() || minY == location.getY() || maxY==location.getY() || minZ==location.getZ() || maxZ==location.getZ()){
            updateBounds();
        }
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
    public boolean removeAll(@NotNull HitBox hitBox) {
        boolean updateBounds = false;
        boolean modified = false;
        for(MovecraftLocation location : hitBox){
            if(locationSet.remove(location)) {
                modified = true;
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

    private final static MovecraftLocation[] SHIFTS = {
            new MovecraftLocation(0, 0, 1),
            new MovecraftLocation(0, 1, 0),
            new MovecraftLocation(1, 0 ,0),
            new MovecraftLocation(0, 0, -1),
            new MovecraftLocation(0, -1, 0),
            new MovecraftLocation(-1, 0, 0)};
    /**
     * finds the axial neighbors to a location. Neighbors are defined as locations that exist within one meter of a given
     * location
     * @param location the location to search for neighbors
     * @return an iterable set of neighbors to the given location
     */
    @NotNull
    public Iterable<MovecraftLocation> neighbors(@NotNull MovecraftLocation location){
        if(this.isEmpty()){
            return Collections.emptyList();
        }
        final List<MovecraftLocation> neighbors = new ArrayList<>(6);
        for(MovecraftLocation test : SHIFTS){
            if(this.contains(location.add(test))){
                neighbors.add(location.add(test));
            }
        }
        return neighbors;
    }

    /**
     * Gets a HitBox that represents the "exterior" of this HitBox. The exterior is defined as the region of all
     * location accessible from the six bounding planes of the hitbox before encountering a location contained in the
     * original HitBox. Functions similarly to a flood fill but in three dimensions
     * @return the exterior HitBox
     */
    public HashHitBox exterior(){
        return null;
    }

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
