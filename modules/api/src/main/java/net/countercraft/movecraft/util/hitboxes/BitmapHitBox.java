package net.countercraft.movecraft.util.hitboxes;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.exception.EmptyHitBoxException;
import org.jetbrains.annotations.NotNull;
import org.roaringbitmap.longlong.LongIterator;
import org.roaringbitmap.longlong.Roaring64NavigableMap;

import java.util.Collection;
import java.util.Iterator;

public class BitmapHitBox implements MutableHitBox {
    private final Roaring64NavigableMap backing;
    private final Long2IntMap localMinY;
    private boolean invalidateBounds = false;
    private int minX = Integer.MAX_VALUE;
    private int minY = Integer.MAX_VALUE;
    private int minZ = Integer.MAX_VALUE;
    private int maxX = Integer.MIN_VALUE;
    private int maxY = Integer.MIN_VALUE;
    private int maxZ = Integer.MIN_VALUE;


    public BitmapHitBox() {
        backing = new Roaring64NavigableMap(true);
        localMinY = new Long2IntOpenHashMap();
    }

    private BitmapHitBox(Roaring64NavigableMap backing) {
        this.backing = backing;
        localMinY = new Long2IntOpenHashMap();
        validateBounds();
    }

    public BitmapHitBox(HitBox hitBox){
        backing = new Roaring64NavigableMap(true);
        localMinY = new Long2IntOpenHashMap();
        this.addAll(hitBox);
    }

    public BitmapHitBox(Collection<MovecraftLocation> locations){
        this();
        this.addAll(locations);
    }

    public BitmapHitBox(BitmapHitBox hitBox){
        backing = new Roaring64NavigableMap(true);
        backing.or(hitBox.backing);
        minX = hitBox.minX;
        minY = hitBox.minY;
        minZ = hitBox.minZ;
        maxX = hitBox.maxX;
        maxY = hitBox.maxY;
        maxZ = hitBox.maxZ;
        localMinY = new Long2IntOpenHashMap(hitBox.localMinY);
        invalidateBounds = hitBox.invalidateBounds;
    }


    @Override
    public boolean add(@NotNull MovecraftLocation location) {
        long l = location.pack();
        if (backing.contains(l)) {
            return false;
        }
        if (!invalidateBounds) {
            checkBounds(location);
        }
        backing.add(l);
        return true;
    }

    private void fastAdd(@NotNull MovecraftLocation location){
        if (!invalidateBounds) {
            checkBounds(location);
        }
        backing.add(location.pack());
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends MovecraftLocation> collection) {
        boolean update = false;
        for (MovecraftLocation location : collection) {
            if(update){
                fastAdd(location);
            } else {
                update = add(location);
            }
        }
        return update;
    }

    @Override
    public boolean addAll(@NotNull HitBox hitBox) {
        boolean update = false;
        for (MovecraftLocation location : hitBox) {
            update |= add(location);
        }
        return update;
    }

    public boolean addAll(@NotNull BitmapHitBox hitBox) {
        int size = backing.getIntCardinality();
        backing.or(hitBox.backing);
        invalidateBounds |= (backing.getIntCardinality() != size);
        return backing.getIntCardinality() != size;
    }

    @Override
    public boolean remove(@NotNull MovecraftLocation location) {
        long l = location.pack();
        if (!backing.contains(l)) {
            return false;
        }
        if (location.getX() == minX ||
                location.getY() == minY ||
                location.getZ() == minZ ||
                location.getX() == maxX ||
                location.getY() == maxY ||
                location.getZ() == maxZ ||
                localMinY.getOrDefault((long)location.getX() << 32 | location.getZ(), -1) == location.getY()) {
            invalidateBounds = true;
        }
        backing.removeLong(l);
        return true;
    }

    @Override
    public boolean removeAll(@NotNull Collection<? extends MovecraftLocation> collection) {
        boolean out = false;
        for(MovecraftLocation location : collection){
            out |= remove(location);
        }
        return out;
    }

    @Override
    public boolean removeAll(@NotNull HitBox hitBox) {
        boolean out = false;
        for(MovecraftLocation location : hitBox){
            out |= remove(location);
        }
        return out;
    }

    public boolean removeAll(@NotNull BitmapHitBox hitBox){
        int size = backing.getIntCardinality();
        backing.andNot(hitBox.backing);
        invalidateBounds |= (backing.getIntCardinality() != size);
        return backing.getIntCardinality() != size;
    }

    @Override
    public void clear() {
        backing.clear();
        minX = Integer.MAX_VALUE;
        minY = Integer.MAX_VALUE;
        minZ = Integer.MAX_VALUE;
        maxX = Integer.MIN_VALUE;
        maxY = Integer.MIN_VALUE;
        maxZ = Integer.MIN_VALUE;
        localMinY.clear();
        invalidateBounds = false;
    }

    @Override
    public int getMinX() {
        if(this.isEmpty()){
            throw new EmptyHitBoxException();
        }
        if (invalidateBounds) {
            validateBounds();
        }
        return minX;
    }

    @Override
    public int getMinY() {
        if(this.isEmpty()){
            throw new EmptyHitBoxException();
        }
        if (invalidateBounds) {
            validateBounds();
        }
        return minY;
    }

    @Override
    public int getMinZ() {
        if(this.isEmpty()){
            throw new EmptyHitBoxException();
        }
        if (invalidateBounds) {
            validateBounds();
        }
        return minZ;
    }

    @Override
    public int getMaxX() {
        if(this.isEmpty()){
            throw new EmptyHitBoxException();
        }
        if (invalidateBounds) {
            validateBounds();
        }
        return maxX;
    }

    @Override
    public int getMaxY() {
        if(this.isEmpty()){
            throw new EmptyHitBoxException();
        }
        if (invalidateBounds) {
            validateBounds();
        }
        return maxY;
    }

    @Override
    public int getMaxZ() {
        if(this.isEmpty()){
            throw new EmptyHitBoxException();
        }
        if (invalidateBounds) {
            validateBounds();
        }
        return maxZ;
    }

    public int getMinYAt(int x, int z){
        if(this.isEmpty()){
            throw new EmptyHitBoxException();
        }
        if (invalidateBounds) {
            validateBounds();
        }
        return localMinY.getOrDefault((long)x << 32 | z, -1);
    }

    @Override
    public int size() {
        return backing.getIntCardinality();
    }

    @Override
    public boolean isEmpty() {
        return backing.isEmpty();
    }

    @Override
    public boolean contains(int x, int y, int z) {
        return backing.contains(MovecraftLocation.pack(x,y,z));
    }

    @NotNull
    @Override
    public Iterator<MovecraftLocation> iterator() {
        return new Iterator<MovecraftLocation>() {
            final LongIterator backingIterator = backing.getLongIterator();
            @Override
            public boolean hasNext() {
                return backingIterator.hasNext();
            }

            @Override
            public MovecraftLocation next() {
                return MovecraftLocation.unpack(backingIterator.next());
            }
        };
    }

    @Override
    public boolean contains(@NotNull MovecraftLocation location) {
        return backing.contains(location.pack());
    }

    @Override
    public boolean containsAll(Collection<? extends MovecraftLocation> collection) {
        for (MovecraftLocation location : collection) {
            if (!contains(location)) {
                return false;
            }
        }
        return true;
    }

    @Override
    @NotNull
    public HitBox difference(HitBox other){
        if(other instanceof BitmapHitBox){
            return this.difference((BitmapHitBox) other);
        }
        return this.difference(new BitmapHitBox(other));
    }
    
    public BitmapHitBox difference(BitmapHitBox other) {
        Roaring64NavigableMap intermediary = new Roaring64NavigableMap(true);
        intermediary.or(this.backing);
        intermediary.andNot(other.backing);
        return new BitmapHitBox(intermediary);
    }
    
    @NotNull
    @Override
    public HitBox intersection(HitBox other){
        return this.intersection(new BitmapHitBox(other));
    }

    public BitmapHitBox intersection(BitmapHitBox other) {
        Roaring64NavigableMap intermediary = new Roaring64NavigableMap(true);
        intermediary.or(this.backing);
        intermediary.and(other.backing);
        return new BitmapHitBox(intermediary);
    }

    @NotNull
    @Override
    public HitBox union(HitBox other){
        return this.union(new BitmapHitBox(other));
    }
    
    public BitmapHitBox union(BitmapHitBox other) {
        Roaring64NavigableMap intermediary = new Roaring64NavigableMap(true);
        intermediary.or(this.backing);
        intermediary.or(other.backing);
        return new BitmapHitBox(intermediary);
    }

    @NotNull
    @Override
    public HitBox symmetricDifference(HitBox other){
        return this.symmetricDifference(new BitmapHitBox(other));
    }
    
    public BitmapHitBox symmetricDifference(BitmapHitBox other) {
        Roaring64NavigableMap intermediary = new Roaring64NavigableMap(true);
        intermediary.or(this.backing);
        intermediary.xor(other.backing);
        return new BitmapHitBox(intermediary);
    }

    private void validateBounds() {
        backing.forEach((it) -> checkBounds(MovecraftLocation.unpack(it)));
        invalidateBounds = false;
    }

    private void checkBounds(@NotNull MovecraftLocation location) {
        if (location.getX() > maxX) {
            maxX = location.getX();
        }
        if (location.getY() > maxY) {
            maxY = location.getY();
        }
        if (location.getZ() > maxZ) {
            maxZ = location.getZ();
        }
        if (location.getX() < minX) {
            minX = location.getX();
        }
        if (location.getY() < minY) {
            minY = location.getY();
        }
        if (location.getZ() < minZ) {
            minZ = location.getZ();
        }
        if(location.getY() < localMinY.getOrDefault((long)location.getX() << 32 | location.getZ(), 256)){
            localMinY.put((long)location.getX() << 32 | location.getZ(), location.getY());
        }
    }
}
