package net.countercraft.movecraft.util.hitboxes;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.exception.EmptyHitBoxException;
import net.countercraft.movecraft.util.collections.LocationSet;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;

public class TreeHitBox implements MutableHitBox{
    private final LocationSet locations = new LocationSet();
    private final Long2IntMap localMinY;
    private boolean invalidateBounds = false;
    private int minX = Integer.MAX_VALUE;
    private int minY = Integer.MAX_VALUE;
    private int minZ = Integer.MAX_VALUE;
    private int maxX = Integer.MIN_VALUE;
    private int maxY = Integer.MIN_VALUE;
    private int maxZ = Integer.MIN_VALUE;

    public TreeHitBox() {
        localMinY = new Long2IntOpenHashMap();
    }

    public TreeHitBox(HitBox box){
        this();
        this.addAll(box);
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

    @Override
    public int size() {
        return locations.size();
    }

    @NotNull
    @Override
    public Iterator<MovecraftLocation> iterator() {
        return locations.iterator();
    }

    @Override
    public boolean contains(@NotNull MovecraftLocation location) {
        return locations.contains(location);
    }

    @Override
    public boolean containsAll(Collection<? extends MovecraftLocation> collection) {
        return locations.containsAll(collection);
    }

    @NotNull
    @Override
    public HitBox difference(HitBox other) {
        var out = new TreeHitBox();
        for(var location : this){
            if(!other.contains(location)){
                out.add(location);
            }
        }
        return out;
    }

    @NotNull
    @Override
    public HitBox intersection(HitBox other) {
        var smaller = other.size() < this.size() ? other : this;
        var larger = this == smaller ? other : this;
        var out = new TreeHitBox();
        for(var location : smaller){
            if(larger.contains(location)){
                out.add(location);
            }
        }
        return out;
    }

    @NotNull
    @Override
    public HitBox union(HitBox other) {
        var out = new TreeHitBox(this);
        out.addAll(other);
        return out;
    }

    @NotNull
    @Override
    public HitBox symmetricDifference(HitBox other) {
        var out = new TreeHitBox();
        for(var location : this){
            if(!other.contains(location)){
                out.add(location);
            }
        }
        for(var location : other){
            if(!this.contains(location)){
                out.add(location);
            }
        }
        return out;
    }

    @Override
    public boolean add(@NotNull MovecraftLocation location) {
        if (!invalidateBounds) {
            checkBounds(location);
        }
        return locations.add(location);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends MovecraftLocation> collection) {
        if (!invalidateBounds) {
            collection.forEach(this::checkBounds);
        }
        return locations.addAll(collection);
    }

    @Override
    public boolean addAll(@NotNull HitBox hitBox) {
        return addAll(hitBox.asSet());
    }

    @Override
    public boolean remove(@NotNull MovecraftLocation location) {
        boolean out = locations.remove(location);
        invalidateBounds |= out;
        return out;
    }

    @Override
    public boolean removeAll(@NotNull Collection<? extends MovecraftLocation> collection) {
        boolean out = locations.removeAll(collection);
        invalidateBounds |= out;
        return out;
    }

    @Override
    public boolean removeAll(@NotNull HitBox hitBox) {
        boolean out = locations.removeAll(hitBox.asSet());
        invalidateBounds |= out;
        return out;
    }

    @Override
    public void clear() {
        locations.clear();
        invalidateBounds = false;
        localMinY.clear();
        minX = Integer.MAX_VALUE;
        minY = Integer.MAX_VALUE;
        minZ = Integer.MAX_VALUE;
        maxX = Integer.MIN_VALUE;
        maxY = Integer.MIN_VALUE;
        maxZ = Integer.MIN_VALUE;
    }

    private void validateBounds() {
        locations.forEach(this::checkBounds);
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
