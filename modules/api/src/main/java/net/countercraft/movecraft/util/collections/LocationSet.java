package net.countercraft.movecraft.util.collections;

import net.countercraft.movecraft.MovecraftLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class LocationSet extends AbstractSet<MovecraftLocation> implements Set<MovecraftLocation> {

    private static final int LOW_MASK_LENGTH = 16;
    private static final int TREE_DEPTH = 12;
    private static final int TREE_MASK_LENGTH = (64 - LOW_MASK_LENGTH)/TREE_DEPTH;
    private static final int LOW_MASK  = 0b1111111111111111;
    private static final long HIGH_MASK = ~((long)LOW_MASK);

    private final BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitSet>>>>>>>>>>>> tree = new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitSet(LOW_MASK + 1)))))))))))));
    private int size = 0;
    
    public LocationSet(){}

    public LocationSet(Set<MovecraftLocation> other){
        super();
        this.addAll(other);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean contains(Object o) {
        if(o instanceof MovecraftLocation){
            MovecraftLocation location = (MovecraftLocation) o;
            long packed = location.pack();
            var suffix = this.getPrefixLeafIfPresent(packed);
            if(suffix == null){
                return false;
            }
            return suffix.get((int) location.pack() & LOW_MASK);
        }
        return false;
    }

    @NotNull
    @Override
    public Iterator<MovecraftLocation> iterator() {
        //TODO: remove this unholy abomination
        int[] indices = new int[TREE_DEPTH];
        LinkedList<MovecraftLocation> locations = new LinkedList<>();
        for(indices[0] = 0; indices[0] < BitTreeNode.TREE_WIDTH; indices[0]++){
            var A = tree.getIfPresent(indices[0]);
            if(A == null){
                continue;
            }
            for(indices[1]= 0; indices[1]< BitTreeNode.TREE_WIDTH; indices[1]++){
                var B = A.getIfPresent(indices[1]);
                if(B == null){
                    continue;
                }
                for(indices[2] = 0; indices[2] < BitTreeNode.TREE_WIDTH; indices[2]++){
                    var C = B.getIfPresent(indices[2]);
                    if(C == null){
                        continue;
                    }
                    for(indices[3] = 0; indices[3] < BitTreeNode.TREE_WIDTH; indices[3]++){
                        var D = C.getIfPresent(indices[3]);
                        if(D == null){
                            continue;
                        }
                        for(indices[4] = 0; indices[4] < BitTreeNode.TREE_WIDTH; indices[4]++){
                            var E = D.getIfPresent(indices[4]);
                            if(E == null){
                                continue;
                            }
                            for(indices[5] = 0; indices[5] < BitTreeNode.TREE_WIDTH; indices[5]++){
                                var F = E.getIfPresent(indices[5]);
                                if(F == null){
                                    continue;
                                }
                                for(indices[6] = 0; indices[6] < BitTreeNode.TREE_WIDTH; indices[6]++){
                                    var G = F.getIfPresent(indices[6]);
                                    if(G == null){
                                        continue;
                                    }
                                    for(indices[7] = 0; indices[7] < BitTreeNode.TREE_WIDTH; indices[7]++){
                                        var H = G.getIfPresent(indices[7]);
                                        if(H == null){
                                            continue;
                                        }
                                        for(indices[8] = 0; indices[8] < BitTreeNode.TREE_WIDTH; indices[8]++){
                                            var I = H.getIfPresent(indices[8]);
                                            if(I == null){
                                                continue;
                                            }
                                            for(indices[9] = 0; indices[9] < BitTreeNode.TREE_WIDTH; indices[9]++){
                                                var J = I.getIfPresent(indices[9]);
                                                if(J == null){
                                                    continue;
                                                }
                                                for(indices[10] = 0; indices[10] < BitTreeNode.TREE_WIDTH; indices[10]++){
                                                    var K = J.getIfPresent(indices[10]);
                                                    if(K == null){
                                                        continue;
                                                    }
                                                    for(indices[11] = 0; indices[11] < BitTreeNode.TREE_WIDTH; indices[11]++){
                                                        var bs = K.getIfPresent(indices[11]);
                                                        if(bs == null){
                                                            continue;
                                                        }
                                                        for (int index = bs.nextSetBit(0); index >= 0 && index < LOW_MASK+1; index = bs.nextSetBit(index+1)) {
                                                            long out = 0;
                                                            for(int depth = 0; depth < TREE_DEPTH; depth++){
                                                                out = (out << TREE_MASK_LENGTH) ^ indices[11-depth];
                                                            }
                                                            out <<= LOW_MASK_LENGTH;
                                                            out ^= index;
                                                            locations.add(MovecraftLocation.unpack(out));
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return locations.iterator();
    }

    @Override
    public boolean add(MovecraftLocation location) {
        long packed = location.pack();
        var leaf = this.getPrefixLeaf(packed);
        boolean out = !leaf.get((int)packed & LOW_MASK);
        leaf.set((int)packed & LOW_MASK);
        if(out){
            size += 1;
        }
        return out;
    }

    @Override
    public boolean remove(Object o) {
        if(!(o instanceof MovecraftLocation)){
            return false;
        }
        long packed = ((MovecraftLocation) o).pack();
        var leaf = this.getPrefixLeaf(packed);
        boolean out = leaf.get((int)packed & LOW_MASK);
        leaf.set((int)packed & LOW_MASK, false);
        if(out){
            size -= 1;
        }
        return out;
    }

    @Override
    public void clear() {

    }

    @Nullable
    private BitSet getPrefixLeafIfPresent(long path){
        BitTreeNode<? extends BitTreeNode<?>> top = tree;
        path >>>= LOW_MASK_LENGTH;
        for(int i = 0; i < TREE_DEPTH - 2; i++) {
            top = (BitTreeNode<? extends BitTreeNode<?>>) top.getIfPresent(path & BitTreeNode.TREE_MASK);
            if (top == null) {
                return null;
            }
            path >>>= TREE_MASK_LENGTH;
        }
        BitTreeNode<BitTreeNode<BitSet>> a = (BitTreeNode<BitTreeNode<BitSet>>) top;
        var b = a.getIfPresent(path & BitTreeNode.TREE_MASK);
        if(b == null){
            return null;
        }
        return b.getIfPresent((path >>> TREE_MASK_LENGTH) & BitTreeNode.TREE_MASK);
    }

    @NotNull
    private BitSet getPrefixLeaf(long path){
        return tree
                .get((path >>>= LOW_MASK_LENGTH) & BitTreeNode.TREE_MASK)
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

    private static class BitTreeNode<T> implements Iterable<T>{
        public static final int TREE_MASK = 0b1111;
        private static final int TREE_WIDTH = TREE_MASK + 1;
        private final Object[] children;
        private final Supplier<T> initializer;

        private BitTreeNode(@NotNull Supplier<T> initializer) {
            children = new Object[TREE_WIDTH];
            this.initializer = initializer;
        }

        @Nullable
        public T getIfPresent(long index){
            return getIfPresent((int) index);
        }

        @Nullable
        public T getIfPresent(int index){
            if(index < 0 || index > TREE_WIDTH){
                throw new IndexOutOfBoundsException(String.format("Index %d must be in range <0,%d>", index, TREE_WIDTH));
            }
            return (T) children[index];
        }

        @NotNull
        public T get(long index){
            return get((int) index);
        }

        @NotNull
        public T get(int index){
            if(index < 0 || index > TREE_WIDTH){
                throw new IndexOutOfBoundsException(String.format("Index %d must be in range <0,%d>", index, TREE_WIDTH));
            }
            if(children[index] == null) {
                children[index] = initializer.get();
            }
            return (T) children[index];
        }

        @NotNull
        @Override
        public Iterator<T> iterator() {
            return Arrays.stream(children).filter(Objects::nonNull).map((child) -> (T) child).iterator();
        }

        @NotNull
        public Stream<T> stream(){
            return Arrays.stream(children).filter(Objects::nonNull).map((child) -> (T) child);
        }
    }
}
