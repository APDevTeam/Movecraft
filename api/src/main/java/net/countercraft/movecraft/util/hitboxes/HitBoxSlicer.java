package net.countercraft.movecraft.util.hitboxes;

import com.google.common.collect.Iterators;
import net.countercraft.movecraft.MovecraftLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public class HitBoxSlicer implements Iterable<Iterable<MovecraftLocation>> {
    private final HitBox hitbox;

    public HitBoxSlicer(HitBox hitbox){
        this.hitbox = hitbox;
    }

    @NotNull
    @Override
    public Iterator<Iterable<MovecraftLocation>> iterator() {
        var chunkIterator = new SolidHitBox(
            new MovecraftLocation(hitbox.getMinX(), 0, hitbox.getMinZ()).scalarDivide(16),
            new MovecraftLocation(hitbox.getMaxX(), 0, hitbox.getMaxZ()).scalarDivide(16));
        var minY = hitbox.getMinY();
        var maxY = hitbox.getMaxY();

        return Iterators.transform(chunkIterator.iterator(), location -> new Slice(hitbox, location.scalarMultiply(16), minY, maxY));
    }

    private static final class Slice implements Iterable<MovecraftLocation>{
        private final SolidHitBox bounds;
        private final HitBox oracle;

        public Slice(HitBox basis, MovecraftLocation start, int minY, int maxY){
            bounds = new SolidHitBox(
                start.hadamardProduct(1,0,1).translate(0,minY,0),
                start.translate(15, maxY, 15));
            oracle = basis;
        }

        @NotNull
        @Override
        public Iterator<MovecraftLocation> iterator() {
            return oracle.intersection(bounds).iterator();
        }
    }
}
