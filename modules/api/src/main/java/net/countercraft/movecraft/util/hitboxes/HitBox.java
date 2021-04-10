package net.countercraft.movecraft.util.hitboxes;

import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.exception.EmptyHitBoxException;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public interface HitBox extends Iterable<MovecraftLocation>{

    public int getMinX();
    public int getMinY();
    public int getMinZ();
    public int getMaxX();
    public int getMaxY();
    public int getMaxZ();

    default public int getXLength(){
        if(this.isEmpty()){
            return 0;
        }
        return Math.abs(this.getMaxX()-this.getMinX());
    }
    default public int getYLength(){
        if(this.isEmpty()){
            return 0;
        }
        return Math.abs(this.getMaxY()-this.getMinY());
    }
    default public int getZLength(){
        if(this.isEmpty()){
            return 0;
        }
        return Math.abs(this.getMaxZ()-this.getMinZ());
    }

    default public boolean isEmpty(){
        return this.size() == 0;
    }
    public int size();

    private static int average(int high, int low){
        return (high&low) + (high^low)/2;
    }

    @NotNull
    default public MovecraftLocation getMidPoint(){
        if(this.isEmpty()){
            throw new EmptyHitBoxException();
        }
        return new MovecraftLocation(average(getMaxX(), getMinX()), average(getMaxY(), getMinY()),average(getMaxZ(), getMinZ()));
    }

    @NotNull
    @Override
    public Iterator<MovecraftLocation> iterator();

    public boolean contains(@NotNull MovecraftLocation location);

    default boolean contains(int x, int y, int z){
        return this.contains(new MovecraftLocation(x,y,z));
    }

    boolean containsAll(Collection<? extends MovecraftLocation> collection);

    default boolean inBounds(double x, double y, double z){
        if(this.isEmpty()){
            return false;
        }
        return x >= this.getMinX() && x <= this.getMaxX() &&
                y >= this.getMinY() && y <= this.getMaxY()&&
                z >= this.getMinZ() && z <= this.getMaxZ();
    }

    default boolean inBounds(MovecraftLocation location){
        return this.inBounds(location.getX(),location.getY(),location.getZ());
    }

    @NotNull
    default SolidHitBox boundingHitBox(){
        return new SolidHitBox(new MovecraftLocation(this.getMinX(),this.getMinY(),this.getMinZ()),
                new MovecraftLocation(this.getMaxX(),this.getMaxY(),this.getMaxZ()));
    }

    @NotNull
    default Set<MovecraftLocation> asSet(){
        return new HitBoxSetView(this);
    }

    @NotNull
    HitBox difference(HitBox other);

    @NotNull
    HitBox intersection(HitBox other);

    @NotNull
    HitBox union(HitBox other);

    @NotNull
    HitBox symmetricDifference(HitBox other);

    int getMinYAt(int x, int z);

    class HitBoxSetView extends AbstractSet<MovecraftLocation> {
        private final HitBox backing;

        public HitBoxSetView(HitBox backing) {
            this.backing = backing;
        }

        @NotNull
        @Override
        public UnmodifiableIterator<MovecraftLocation> iterator() {
            return Iterators.unmodifiableIterator(backing.iterator());
        }

        @Override
        public int size() {
            return backing.size();
        }

        @Override
        public boolean contains(Object location) {
            return location instanceof MovecraftLocation && backing.contains((MovecraftLocation) location);
        }
    }
}

