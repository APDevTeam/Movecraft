package net.countercraft.movecraft.utils;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.exception.EmptyHitBoxException;
import org.jetbrains.annotations.NotNull;
import org.roaringbitmap.longlong.LongIterator;
import org.roaringbitmap.longlong.Roaring64NavigableMap;

import java.util.Collection;
import java.util.Iterator;

public class BitmapHitBox implements MutableHitBox {
    private final Roaring64NavigableMap backing;
    private boolean invalidateBounds = false;
    private int minX = Integer.MAX_VALUE;
    private int minY = Integer.MAX_VALUE;
    private int minZ = Integer.MAX_VALUE;
    private int maxX = Integer.MIN_VALUE;
    private int maxY = Integer.MIN_VALUE;
    private int maxZ = Integer.MIN_VALUE;


    public BitmapHitBox() {
        backing = new Roaring64NavigableMap(true);
    }

    private BitmapHitBox(Roaring64NavigableMap backing) {
        this.backing = backing;
    }

    public BitmapHitBox(HitBox hitBox){
        backing = new Roaring64NavigableMap(true);
        this.addAll(hitBox);
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

    @Override
    public boolean addAll(@NotNull Collection<? extends MovecraftLocation> collection) {
        boolean update = false;
        for (MovecraftLocation location : collection) {
            update |= add(location);
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
                location.getZ() == maxZ) {
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

    public int getLocalMinY(int x, int z){
        int out = Integer.MAX_VALUE;
        for(MovecraftLocation location : this){
            if(location.getZ() == z && location.getX() == x && location.getY() < out){
                out = location.getY();
            }
        }
        return out;
    }

    @Override
    public int size() {
        return backing.getIntCardinality();
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

    public BitmapHitBox difference(BitmapHitBox other) {
        Roaring64NavigableMap intermediary = new Roaring64NavigableMap(true);
        intermediary.or(this.backing);
        intermediary.andNot(other.backing);
        return new BitmapHitBox(intermediary);
    }

    public BitmapHitBox intersection(BitmapHitBox other) {
        Roaring64NavigableMap intermediary = new Roaring64NavigableMap(true);
        intermediary.or(this.backing);
        intermediary.and(other.backing);
        return new BitmapHitBox(intermediary);
    }

    public BitmapHitBox union(BitmapHitBox other) {
        Roaring64NavigableMap intermediary = new Roaring64NavigableMap(true);
        intermediary.or(this.backing);
        intermediary.or(other.backing);
        return new BitmapHitBox(intermediary);
    }

    public BitmapHitBox symetricDifference(BitmapHitBox other) {
        Roaring64NavigableMap intermediary = new Roaring64NavigableMap(true);
        intermediary.or(this.backing);
        intermediary.xor(other.backing);
        return new BitmapHitBox(intermediary);
    }

    private void validateBounds() {
        backing.forEach((it) -> checkBounds(MovecraftLocation.unpack(it)));
        invalidateBounds = false;
    }

    private void checkBounds(MovecraftLocation location) {
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
    }
}
