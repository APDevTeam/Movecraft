package net.countercraft.movecraft.utils;

import com.google.common.collect.MinMaxPriorityQueue;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.exception.EmptyHitBoxException;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
@Deprecated
public class HashHitBox implements MutableHitBox {
    private final Set<MovecraftLocation> locationSet = new HashSet<>();
//    private int minX,maxX,minY,maxY,minZ,maxZ;

    private MinMaxPriorityQueue<MovecraftLocation> xQueue = MinMaxPriorityQueue.orderedBy(Comparator.comparingInt(MovecraftLocation::getX)).create();
    private MinMaxPriorityQueue<MovecraftLocation> yQueue = MinMaxPriorityQueue.orderedBy((Comparator.comparingInt(MovecraftLocation::getY))).create();
    private MinMaxPriorityQueue<MovecraftLocation> zQueue = MinMaxPriorityQueue.orderedBy(Comparator.comparingInt(MovecraftLocation::getZ)).create();

//    private HashMap<IntPair, TreeSet<MovecraftLocation>> xyPlane = new HashMap<>();
    private HashMap<IntPair, BitSet> xzPlane = new HashMap<>();
//    private HashMap<IntPair, TreeSet<MovecraftLocation>> yzPlane = new HashMap<>();
    private boolean differBounds = true;

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
        initBounds();
        return xQueue.peekFirst().getX();
    }

    public int getMaxX() {
        if(locationSet.isEmpty()){
            throw new EmptyHitBoxException();
        }
        initBounds();
        return xQueue.peekLast().getX();
    }

    public int getMinY() {
        if(locationSet.isEmpty()){
            throw new EmptyHitBoxException();
        }
        initBounds();
        return yQueue.peekFirst().getY();
    }

    public int getMaxY() {
        if(locationSet.isEmpty()){
            throw new EmptyHitBoxException();
        }
        initBounds();
        return yQueue.peekLast().getY();
    }

    public int getMinZ() {
        if(locationSet.isEmpty()){
            throw new EmptyHitBoxException();
        }
        initBounds();
        return zQueue.peekFirst().getZ();
    }

    public int getMaxZ() {
        if(locationSet.isEmpty()){
            throw new EmptyHitBoxException();
        }
        initBounds();
        return zQueue.peekLast().getZ();
    }

    public int getXLength(){
        if(locationSet.isEmpty()){
            return 0;
        }
        return Math.abs(getMaxX()-getMinX());
    }

    public int getYLength(){
        if(locationSet.isEmpty()){
            return 0;
        }
        return getMaxY()-getMinY();
    }

    public int getZLength(){
        if(locationSet.isEmpty()){
            throw new EmptyHitBoxException();
        }
        return Math.abs(getMaxZ()-getMinZ());
    }

    public int getLocalMaxY(int x, int z){
        if(locationSet.isEmpty()){
            throw new EmptyHitBoxException();
        }
        initBounds();
        IntPair point = new IntPair(x,z);
        if(!xzPlane.containsKey(point) || xzPlane.get(point).isEmpty() ){
            return -1;
        }
        return xzPlane.get(point).previousSetBit(xzPlane.get(point).size());
    }

    public int getLocalMinY(int x, int z){
        if(locationSet.isEmpty()){
            throw new EmptyHitBoxException();
        }
        initBounds();
        IntPair point = new IntPair(x,z);
        if(!xzPlane.containsKey(point) || xzPlane.get(point).isEmpty()){
            return -1;
        }
        return xzPlane.get(point).nextSetBit(0);
    }

    @NotNull
    public MovecraftLocation getMidPoint(){
        if(locationSet.isEmpty()){
            throw new EmptyHitBoxException();
        }
        return new MovecraftLocation((getMinX()+getMaxX())/2, (getMinY()+getMaxY())/2,(getMinZ()+getMaxZ())/2);
    }

    public boolean inBounds(MovecraftLocation location){
        if(locationSet.isEmpty()){
            return false;
        }
        return location.getX() >= getMinX() && location.getX() <= getMaxX() &&
                location.getY() >= getMinY() && location.getY() <= getMaxY() &&
                location.getZ() >= getMinZ() && location.getZ() <= getMaxZ();
    }

    public boolean inBounds(double x, double y, double z){
        if(locationSet.isEmpty()){
            return false;
        }
        return x >= getMinX() && x <= getMaxX() &&
                y >= getMinY() && y <= getMaxY() &&
                z >= getMinZ() && z <= getMaxZ();
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
        if(!differBounds){
            xQueue.add(movecraftLocation);
            yQueue.add(movecraftLocation);
            zQueue.add(movecraftLocation);
            initPlanes(movecraftLocation);
            xzPlane.get(new IntPair(movecraftLocation, Plane.XZ)).set(movecraftLocation.getY());
        }
        return locationSet.add(movecraftLocation);
    }

    @Override
    public boolean remove(@NotNull MovecraftLocation location) {
        if(!locationSet.contains(location))
            return false;
        locationSet.remove(location);
        xQueue.remove(location);
        yQueue.remove(location);
        zQueue.remove(location);
        IntPair point = new IntPair(location, Plane.XZ);
        if(xzPlane.containsKey(point)){
            xzPlane.get(point).clear(location.getY());
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
        boolean modified = false;
        for (MovecraftLocation location : c) {
            if (remove(location))
                modified = true;
        }
        return modified;
    }

    @Override
    public boolean removeAll(@NotNull HitBox hitBox) {
        boolean modified = false;
        for (MovecraftLocation location : hitBox) {
            if (remove(location))
                modified = true;
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

    @NotNull @Override
    public Set<MovecraftLocation> asSet(){
        return Collections.unmodifiableSet(this.locationSet);
    }

    private void initBounds(){
        if(!differBounds) return;
        differBounds = false;
        addAll(this);
    }

    private void initPlanes(@NotNull MovecraftLocation location){
//        xyPlane.putIfAbsent(new IntPair(location, Plane.XY), new TreeSet<>(Comparator.comparingInt(MovecraftLocation::getZ)));
        xzPlane.putIfAbsent(new IntPair(location, Plane.XZ), new BitSet(256));
//        yzPlane.putIfAbsent(new IntPair(location, Plane.YZ), new TreeSet<>(Comparator.comparingInt(MovecraftLocation::getX)));
    }
    private enum Plane{
        XY,XZ,YZ
    }
    private class IntPair {

        final int i,j;
        private IntPair(@NotNull MovecraftLocation location, @NotNull Plane plain){
            switch (plain){
                case XY:
                    i = location.getX();
                    j = location.getY();
                    break;
                case XZ:
                    i = location.getX();
                    j = location.getZ();
                    break;
                case YZ:
                    i = location.getY();
                    j = location.getZ();
                    break;
                default:
                    throw new NullPointerException();
            }
        }

        private IntPair(int i, int j){
            this.i = i;
            this.j = j;
        }

        public int getI(){
            return i;
        }

        public int getJ(){
            return j;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            IntPair intPair = (IntPair) o;
            return i == intPair.i &&
                    j == intPair.j;
        }

        @Override
        public int hashCode() {
            return Objects.hash(i, j);
        }
    }
}
