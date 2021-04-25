package net.countercraft.movecraft.util.collections;

import net.countercraft.movecraft.MovecraftLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Deprecated
public class LocationSet extends AbstractSet<MovecraftLocation> implements Set<MovecraftLocation> {

    private static final int LOW_MASK_LENGTH = 16;
    private static final int TREE_DEPTH = 12;
    private static final int TREE_MASK_LENGTH = (64 - LOW_MASK_LENGTH)/TREE_DEPTH;
    private static final int LOW_MASK  = 0b1111111111111111;
    private static final long HIGH_MASK = ~((long)LOW_MASK);

    private final BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitSet>>>>>>>>>>>> tree = new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitSet(LOW_MASK + 1)))))))))))));
    private int size = 0;

    public LocationSet(){}

    public LocationSet(Collection<MovecraftLocation> other){
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

    @SuppressWarnings("ConstantConditions")
    @NotNull
    @Override
    public Iterator<MovecraftLocation> iterator() {
        if(isEmpty()){
            return Collections.emptyIterator();
        }
        // Initialize our state and the first output
        // Where possible we use getIfPresent as it is more efficient
        int[] childIndex = new int[TREE_DEPTH];
        BitTreeNode<?>[] nodeAtDepth = new BitTreeNode[TREE_DEPTH];
        nodeAtDepth[0] = tree;
        for(int i = 0; i < TREE_DEPTH-1; i++){
            childIndex[i] = nodeAtDepth[i].nextSetChild(0);
            nodeAtDepth[i+1] = (BitTreeNode<?>) nodeAtDepth[i].getIfPresent(childIndex[i]);
        }
        childIndex[TREE_DEPTH-1] = nodeAtDepth[TREE_DEPTH-1].nextSetChild(0);
        final BitSet[] set = {(BitSet) nodeAtDepth[TREE_DEPTH - 1].getIfPresent(childIndex[TREE_DEPTH - 1])};
        final int[] bitIndex = {set[0].nextSetBit(0)};
        return new Iterator<>(){

            @Override
            public boolean hasNext() {
                return childIndex[0] != -1;
            }

            @Override
            public MovecraftLocation next() {
                // Pack the previously known correct output
                long packed = getPacked();
                // Check if we can iterate through our leaf bitset
                bitIndex[0] = set[0].nextSetBit(bitIndex[0] + 1);
                if(bitIndex[0] != -1){
                    return MovecraftLocation.unpack(packed);
                }
                // Ascend up the tree to find the next set node
                int i;
                for(i = TREE_DEPTH-1; i >= 0;) {
                    childIndex[i] = nodeAtDepth[i].nextSetChild(childIndex[i] + 1);
                    if (childIndex[i] == -1) {
                        i--;
                    } else {
                        break;
                    }
                }
                // If no such node exists, we're done
                if(!hasNext()){
                    return MovecraftLocation.unpack(packed);
                }
                // Otherwise, descend the tree and update the state to match our path
                nodeAtDepth[i+1] = (BitTreeNode<?>) nodeAtDepth[i].getIfPresent(childIndex[i]);
                i++;
                for(; i < TREE_DEPTH-1; i++) {
                    if(childIndex[i] == -1){
                        childIndex[i] = nodeAtDepth[i].nextSetChild(0);
                        nodeAtDepth[i+1] = (BitTreeNode<?>) nodeAtDepth[i].getIfPresent(childIndex[i]);
                    }
                }
                // The leaf bitset must be calculated separately
                if(childIndex[TREE_DEPTH-1] == -1){
                    childIndex[TREE_DEPTH-1] = nodeAtDepth[TREE_DEPTH-1].nextSetChild(0);
                    set[0] = (BitSet) nodeAtDepth[TREE_DEPTH-1].getIfPresent(childIndex[TREE_DEPTH-1]);
                }
                // Update our next index
                bitIndex[0] = set[0].nextSetBit(0);
                return MovecraftLocation.unpack(packed);
            }

            private long getPacked() {
                long packed=0;
                for(int i = 0; i < TREE_DEPTH; i++){
                    packed |= ((long) childIndex[i]) << (TREE_MASK_LENGTH*(i));
                }
                packed <<= LOW_MASK_LENGTH;
                packed ^= bitIndex[0];
                return packed;
            }
        };
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
            if(leaf.isEmpty()){
                this.remove(packed>>>LOW_MASK_LENGTH, 0, tree);
            }
        }
        return out;
    }

    @SuppressWarnings("ConstantConditions")
    private boolean remove(long path, int depth, @NotNull BitTreeNode<?> parent){
        int childIndex = (int) (path & BitTreeNode.TREE_MASK);
        if(depth == TREE_DEPTH-1){
            parent.children[childIndex] = null;
            return parent.nextSetChild(0) == -1;
        }
        if(remove(path >>> TREE_MASK_LENGTH, depth+1, (BitTreeNode<?>) parent.getIfPresent(childIndex))){
            parent.children[childIndex] = null;
            return parent.nextSetChild(0) == -1;
        }
        return false;
    }

    @Override
    public void clear() {

    }

    @Nullable
    private BitSet getPrefixLeafIfPresent(long path){
        BitTreeNode<?> top = tree;
        path >>>= LOW_MASK_LENGTH;
        for(int i = 0; i < TREE_DEPTH - 2; i++) {
            top = (BitTreeNode<?>) top.getIfPresent(path & BitTreeNode.TREE_MASK);
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
        private final T[] children;
        private final Supplier<T> initializer;

        @SuppressWarnings("unchecked")
        private BitTreeNode(@NotNull Supplier<T> initializer) {
            children = (T[]) new Object[TREE_WIDTH];
            this.initializer = initializer;
        }

        public int nextSetChild(int start){
            for(int i = start; i < TREE_WIDTH; i++){
                if(children[i] != null){
                    return i;
                }
            }
            return -1;
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
            return children[index];
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
            return children[index];
        }

        @NotNull
        @Override
        public Iterator<T> iterator() {
            return Arrays.stream(children).filter(Objects::nonNull).iterator();
        }

        @NotNull
        public Stream<T> stream(){
            return Arrays.stream(children).filter(Objects::nonNull);
        }
    }
}
