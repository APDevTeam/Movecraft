package net.countercraft.movecraft.util.collections;

import net.countercraft.movecraft.MovecraftLocation;
import org.jetbrains.annotations.NotNull;
import org.roaringbitmap.longlong.Roaring64Bitmap;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;

public class BitmapLocationSet extends AbstractSet<MovecraftLocation> {
    private final Roaring64Bitmap backing = new Roaring64Bitmap();

    public BitmapLocationSet(){}

    public BitmapLocationSet(Collection<MovecraftLocation> locations){
        this();
        this.uncheckedAddAll(locations);
    }

    public void uncheckedAdd(@NotNull MovecraftLocation location){
        this.backing.addLong(location.pack());
    }

    public void uncheckedAddAll(@NotNull Collection<? extends MovecraftLocation> locations){
        for(var location : locations){
            this.uncheckedAdd(location);
        }
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends MovecraftLocation> locations){
        boolean out = false;
        for(MovecraftLocation location : locations){
            var packed = location.pack();
            if(!out && !backing.contains(packed)){
                out = true;
            }
            backing.add(packed);
        }
        return out;
    }

    @NotNull
    @Override
    public Iterator<MovecraftLocation> iterator() {
        var iter = backing.getLongIterator();
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

    @Override
    public boolean contains(Object o){
        if(o instanceof MovecraftLocation){
            return backing.contains(((MovecraftLocation) o).pack());
        }
        return false;
    }

    /**
     * Note that in most cases, this will be faster than loop based iteration
     * {@inheritDoc}
     */
    @Override
    public void forEach(Consumer<? super MovecraftLocation> consumer){
        backing.forEach((packed) -> consumer.accept(MovecraftLocation.unpack(packed)));
    }
}
