package net.countercraft.movecraft.util.hitboxes;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.exception.EmptyHitBoxException;
import net.countercraft.movecraft.util.collections.BitmapLocationSet;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

public class SetHitBox implements MutableHitBox{
    private final Set<MovecraftLocation> locations = new LinkedHashSet<>();
    private final Long2IntMap localMinY;
    private boolean invalidateBounds = false;
    private int minX = Integer.MAX_VALUE;
    private int minY = Integer.MAX_VALUE;
    private int minZ = Integer.MAX_VALUE;
    private int maxX = Integer.MIN_VALUE;
    private int maxY = Integer.MIN_VALUE;
    private int maxZ = Integer.MIN_VALUE;

    public SetHitBox() {
        localMinY = new Long2IntOpenHashMap();
    }

    public SetHitBox(HitBox box){
        this();
        Consumer<MovecraftLocation> consumer =  locations::add;
        box.forEach(consumer.andThen(this::checkBounds));
    }

    public SetHitBox(Collection<MovecraftLocation> locations){
        this();
        Consumer<MovecraftLocation> consumer =  this.locations::add;
        locations.forEach(consumer.andThen(this::checkBounds));
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
        var out = new SetHitBox();
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
        var out = new SetHitBox();
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
        var out = new SetHitBox(this);
        out.addAll(other);
        return out;
    }

    @NotNull
    @Override
    public HitBox symmetricDifference(HitBox other) {
        var out = new SetHitBox();
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
        boolean out = locations.add(location);
        if (out && !invalidateBounds) {
            checkBounds(location);
        }
        return out;
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends MovecraftLocation> collection) {
        boolean out = false;
        boolean test;
        for(var location : collection){
            if ((test = locations.add(location)) && !invalidateBounds) {
                this.checkBounds(location);
            }
            out |= test;
        }
        return out;
    }

    @Override
    public boolean addAll(@NotNull HitBox hitBox) {
        boolean out = false;
        boolean test;
        for(var location : hitBox){
            if ((test = locations.add(location)) && !invalidateBounds) {
                this.checkBounds(location);
            }
            out |= test;
        }
        return out;
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
    public void forEach(Consumer<? super MovecraftLocation> consumer){
        this.locations.forEach(consumer);
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

    @Override @NotNull
    public Set<MovecraftLocation> asSet(){
        return Collections.unmodifiableSet(locations);
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
