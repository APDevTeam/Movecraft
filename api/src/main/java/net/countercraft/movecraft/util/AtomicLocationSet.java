package net.countercraft.movecraft.util;

import net.countercraft.movecraft.MovecraftLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;


/**
 * Implements an atomic Set of MovecraftLocations. Note that this implementation is purely incremental.
 */
public class AtomicLocationSet implements Set<MovecraftLocation> {
    private static final int LOW_MASK_LENGTH = 16;
    private static final int TREE_DEPTH = 12;
    private static final int TREE_MASK_LENGTH = (64 - LOW_MASK_LENGTH) / TREE_DEPTH;
    private static final int LOW_MASK  = 0b1111111111111111;
    private static final long HIGH_MASK = ~((long) LOW_MASK);



    private final BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<AtomicBitSet>>>>>>>>>>>> tree
            = new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new AtomicBitSet(LOW_MASK + 1)))))))))))));
    private final LongAdder size = new LongAdder();


    @Override
    public int size() {
        return size.intValue();
    }

    @Override
    public boolean isEmpty() {
        return size.intValue() == 0;
    }

    @Override
    public boolean contains(Object o) {
        if(o instanceof MovecraftLocation) {
            MovecraftLocation location = (MovecraftLocation) o;
            long packed = location.pack();
            var suffix = this.getPrefixLeafIfPresent(packed);
            if(suffix == null)
                return false;

            return suffix.get((int) (packed & LOW_MASK));
        }
        return false;
    }

    @Override
    public boolean add(@NotNull MovecraftLocation location) {
        long packed = location.pack();
        var leaf = this.getPrefixLeaf(packed);
        boolean out = !leaf.add((int) packed & LOW_MASK);
        if(out)
            size.increment();

        return out;
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends MovecraftLocation> c) {
        for(MovecraftLocation location : c) {
            if(!add(location))
                return false;
        }
        return true;
    }

    @Nullable
    private AtomicBitSet getPrefixLeafIfPresent(long path) {
        BitTreeNode<? extends BitTreeNode<?>> top = tree;
        path >>>= LOW_MASK_LENGTH;
        for(int i = 0; i < TREE_DEPTH - 2; i++) {
            top = (BitTreeNode<? extends BitTreeNode<?>>) top.getIfPresent(path & BitTreeNode.TREE_MASK);
            if (top == null)
                return null;

            path >>>= TREE_MASK_LENGTH;
        }
        BitTreeNode<BitTreeNode<AtomicBitSet>> a = (BitTreeNode<BitTreeNode<AtomicBitSet>>) top;
        var b = a.getIfPresent(path & BitTreeNode.TREE_MASK);
        if(b == null)
            return null;

        return b.getIfPresent((path >>> TREE_MASK_LENGTH) & BitTreeNode.TREE_MASK);
    }

    @NotNull
    private AtomicBitSet getPrefixLeaf(long path) {
        path >>>= LOW_MASK_LENGTH - TREE_MASK_LENGTH;
        return tree
                .get((path >>>= TREE_MASK_LENGTH) & BitTreeNode.TREE_MASK)
                .get((path >>>= TREE_MASK_LENGTH) & BitTreeNode.TREE_MASK)
                .get((path >>>= TREE_MASK_LENGTH) & BitTreeNode.TREE_MASK)
                .get((path >>>= TREE_MASK_LENGTH) & BitTreeNode.TREE_MASK)
                .get((path >>>= TREE_MASK_LENGTH) & BitTreeNode.TREE_MASK)
                .get((path >>>= TREE_MASK_LENGTH) & BitTreeNode.TREE_MASK)
                .get((path >>>= TREE_MASK_LENGTH) & BitTreeNode.TREE_MASK)
                .get((path >>>= TREE_MASK_LENGTH) & BitTreeNode.TREE_MASK)
                .get((path >>>= TREE_MASK_LENGTH) & BitTreeNode.TREE_MASK)
                .get((path >>>= TREE_MASK_LENGTH) & BitTreeNode.TREE_MASK)
                .get((path >>>= TREE_MASK_LENGTH) & BitTreeNode.TREE_MASK)
                .get(path >>> TREE_MASK_LENGTH & BitTreeNode.TREE_MASK);
    }


    @Override
    public Object @NotNull [] toArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T[] toArray(T @NotNull [] a) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public Iterator<MovecraftLocation> iterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }



    private static class BitTreeNode<T>{
        public static final int TREE_MASK = 0b1111;
        private static final int TREE_WIDTH = TREE_MASK + 1;



        private final AtomicReferenceArray<T> children;
        private final Supplier<T> initializer;



        private BitTreeNode(@NotNull Supplier<T> initializer) {
            children = new AtomicReferenceArray<>(TREE_WIDTH);
            this.initializer = initializer;
        }

        @Nullable
        public T getIfPresent(long index) {
            return getIfPresent((int) index);
        }

        @Nullable
        public T getIfPresent(int index) {
            if(index < 0 || index > TREE_WIDTH)
                throw new IndexOutOfBoundsException(String.format("Index %d must be in range <0,%d>", index, TREE_WIDTH));

            return children.get(index);
        }

        @NotNull
        public T get(long index) {
            return get((int) index);
        }

        @NotNull
        public T get(int index) {
            Objects.checkIndex(index, TREE_WIDTH);
            var fetch = children.getAcquire(index);
            if(fetch == null) {
                var child = initializer.get();
                fetch = children.compareAndExchangeRelease(index, null, child);
                if(fetch == null)
                    return child;
            }
            return fetch;
        }
    }
}
