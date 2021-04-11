package net.countercraft.movecraft.util.collections;

import net.countercraft.movecraft.MovecraftLocation;
import org.jetbrains.annotations.NotNull;
import org.roaringbitmap.longlong.Roaring64Bitmap;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;

public class BitmapLocationSet extends AbstractSet<MovecraftLocation> {
    private final Roaring64Bitmap backing = new Roaring64Bitmap();

    public BitmapLocationSet(){}

    public BitmapLocationSet(Collection<MovecraftLocation> locations){
        this();
        this.addAll(locations);
    }

    @NotNull
    @Override
    public Iterator<MovecraftLocation> iterator() {
        var iter = backing.iterator();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public MovecraftLocation next() {
                return MovecraftLocation.unpack(iter.next());
            }
        };
    }

    @Override
    public int size() {
        return backing.getIntCardinality();
    }

    @Override
    public boolean add(MovecraftLocation location){
        var packed = location.pack();
        var out = !backing.contains(packed);
        backing.addLong(location.pack());
        return out;
    }

    @Override
    public boolean remove(Object o){
        if(o instanceof MovecraftLocation){
            MovecraftLocation location = (MovecraftLocation) o;
            var packed = location.pack();
            if(!backing.contains(packed)){
                return false;
            }
            backing.removeLong(location.pack());
            return true;
        }
        return false;

    }
}
