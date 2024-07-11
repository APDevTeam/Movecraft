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
    int getMinX();
    int getMinY();
    int getMinZ();
    int getMaxX();
    int getMaxY();
    int getMaxZ();

    default int getXLength() {
        if (isEmpty())
            return 0;

        return Math.abs(getMaxX() - getMinX());
    }

    default int getYLength() {
        if (isEmpty())
            return 0;

        return Math.abs(getMaxY() - getMinY());
    }

    default int getZLength() {
        if (isEmpty())
            return 0;

        return Math.abs(getMaxZ() -getMinZ());
    }

    default boolean isEmpty() {
        return size() == 0;
    }

    int size();

    private static int average(int high, int low) {
        return (high&low) + (high^low) / 2;
    }

    @NotNull
    default MovecraftLocation getMidPoint() throws EmptyHitBoxException {
        if (isEmpty())
            throw new EmptyHitBoxException();

        return new MovecraftLocation(average(getMaxX(), getMinX()), average(getMaxY(), getMinY()),average(getMaxZ(), getMinZ()));
    }

    @NotNull
    @Override
    Iterator<MovecraftLocation> iterator();

    boolean contains(@NotNull MovecraftLocation location);

    default boolean contains(int x, int y, int z) {
        return contains(new MovecraftLocation(x, y, z));
    }

    boolean containsAll(Collection<? extends MovecraftLocation> collection);

    default boolean inBounds(double x, double y, double z) {
        if (isEmpty())
            return false;

        return x >= getMinX() && x <= getMaxX() &&
                y >= getMinY() && y <= getMaxY()&&
                z >= getMinZ() && z <= getMaxZ();
    }

    default boolean inBounds(MovecraftLocation location) {
        return inBounds(location.getX(),location.getY(),location.getZ());
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

