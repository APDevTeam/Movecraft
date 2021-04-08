package net.countercraft.movecraft.util.collections;

import net.countercraft.movecraft.MovecraftLocation;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class LocationTrieSet extends AbstractSet<MovecraftLocation> implements Set<MovecraftLocation> {

    private static final int TREE_DEPTH = 16;
    private static final int TREE_MASK_LENGTH = (Long.BYTES * 8)/TREE_DEPTH;

    private final BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<Boolean>>>>>>>>>>>>>>>> tree = new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<Boolean>(() -> Boolean.TRUE))))))))))))))));
    private int size = 0;

    public LocationTrieSet(){}

    public LocationTrieSet(Set<MovecraftLocation> other){
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
            return contains(packed);
        }
        return false;
    }

    boolean contains(long packed){
        var suffix = this.getPrefixLeafIfPresent(packed);
        if(suffix == null){
            return false;
        }
        int leafIndex = (int) (packed >>> TREE_MASK_LENGTH * (TREE_DEPTH - 1));
        return suffix.getIfPresent(leafIndex, false);
    }

    PrimitiveIterator.OfLong longIterator(){
        if(isEmpty()){
            return new PrimitiveIterator.OfLong(){
                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public long nextLong() {
                    return 0;
                }
            };
        }

        return new LongTrieIterator(this);
    }

    @NotNull
    @Override
    public Iterator<MovecraftLocation> iterator() {
        if(isEmpty()){
            return Collections.emptyIterator();
        }
        var iter = new LongTrieIterator(this);
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public MovecraftLocation next() {
                return MovecraftLocation.unpack(iter.nextLong());
            }
        };
    }

    @Override
    public boolean add(MovecraftLocation location) {
        long packed = location.pack();
        return add(packed);
    }

    boolean add(long packed){
        var leaf = this.getPrefixLeaf(packed);
        int leafIndex = (int) (packed >>> TREE_MASK_LENGTH * (TREE_DEPTH - 1));
        boolean out = !leaf.getIfPresent(leafIndex, false);
        if(out){
            size += 1;
            leaf.get(leafIndex);
        }
        return out;
    }

    @Override
    public boolean remove(Object o) {
        if(!(o instanceof MovecraftLocation)){
            return false;
        }
        long packed = ((MovecraftLocation) o).pack();
        var leaf = this.getPrefixLeafIfPresent(packed);
        if(leaf == null){
            return false;
        }
        int leafIndex = (int) (packed >>> TREE_MASK_LENGTH * (TREE_DEPTH - 1));
        boolean out = leaf.getIfPresent(leafIndex, Boolean.FALSE);
        leaf.remove(leafIndex);
        if(out){
            size -= 1;
            if(leaf.nextSetChild(0) == -1){
                this.remove(packed, 0, tree);
            }
        }
        return out;
    }

    @SuppressWarnings("ConstantConditions")
    private boolean remove(long path, int depth, @NotNull BitTreeNode<?> parent){
        int childIndex = (int) (path & BitTreeNode.TREE_MASK);
        if(depth == TREE_DEPTH-1){
            parent.remove(childIndex);
            return parent.nextSetChild(0) == -1;
        }
        if(remove(path >>> TREE_MASK_LENGTH, depth+1, (BitTreeNode<?>) parent.getIfPresent(childIndex))){
            parent.remove(childIndex);
            return parent.nextSetChild(0) == -1;
        }
        return false;
    }

    @Override
    public void clear() {

    }

    @Nullable
    private BitTreeNode<Boolean> getPrefixLeafIfPresent(long path){
        BitTreeNode<?> top = tree;
        int i;
        int currentPath = (int) path;
        for(i = 0; i < 8; i++) {
            top = (BitTreeNode<?>) top.getIfPresent(currentPath & BitTreeNode.TREE_MASK);
            if (top == null) {
                return null;
            }
            currentPath >>>= TREE_MASK_LENGTH;
        }
        currentPath = (int) (path >>> 32);
        for(; i < TREE_DEPTH - 1; i++) {
            top = (BitTreeNode<?>) top.getIfPresent(currentPath & BitTreeNode.TREE_MASK);
            if (top == null) {
                return null;
            }
            currentPath >>>= TREE_MASK_LENGTH;
        }

        return (BitTreeNode<Boolean>) top;
    }

    @NotNull
    private BitTreeNode<Boolean> getPrefixLeaf(long path){
        int upper = (int) (path >>> 32);
        int lower = (int) path;
        return tree
                .get((lower) & BitTreeNode.TREE_MASK)
                .get((lower >>>= TREE_MASK_LENGTH) & BitTreeNode.TREE_MASK)
                .get((lower >>>= TREE_MASK_LENGTH) & BitTreeNode.TREE_MASK)
                .get((lower >>>= TREE_MASK_LENGTH) & BitTreeNode.TREE_MASK)
                .get((lower >>>= TREE_MASK_LENGTH) & BitTreeNode.TREE_MASK)
                .get((lower >>>= TREE_MASK_LENGTH) & BitTreeNode.TREE_MASK)
                .get((lower >>>= TREE_MASK_LENGTH) & BitTreeNode.TREE_MASK)
                .get(lower >>> TREE_MASK_LENGTH & BitTreeNode.TREE_MASK)
                .get(upper & BitTreeNode.TREE_MASK)
                .get((upper >>>= TREE_MASK_LENGTH) & BitTreeNode.TREE_MASK)
                .get((upper >>>= TREE_MASK_LENGTH) & BitTreeNode.TREE_MASK)
                .get((upper >>>= TREE_MASK_LENGTH) & BitTreeNode.TREE_MASK)
                .get((upper >>>= TREE_MASK_LENGTH) & BitTreeNode.TREE_MASK)
                .get((upper >>>= TREE_MASK_LENGTH) & BitTreeNode.TREE_MASK)
                .get(upper >>> TREE_MASK_LENGTH & BitTreeNode.TREE_MASK);
    }

     private static class BitTreeNode<T> implements Iterable<T>{
         // Disabled by default, checking bounds is slow and this is an internal api
        private static final boolean ENABLE_SAFETY = false;
        public static final int TREE_MASK = 0b1111;
        private static final int TREE_WIDTH = TREE_MASK + 1;
        private final T[] children;
        private final Supplier<T> initializer;
        private int status = 0;

        @SuppressWarnings("unchecked")
        private BitTreeNode(@NotNull Supplier<T> initializer) {
            children = (T[]) new Object[TREE_WIDTH];
            this.initializer = initializer;
        }

        public int nextSetChild(int start){
            int masked = status & (0xffffffff << start);
            if(masked == 0){
                return -1;
            }
            return Integer.numberOfTrailingZeros(masked);
        }

         @Contract("_, !null -> !null")
        public T getIfPresent(int index, T defaultValue){
            var out = getIfPresent(index);
            return out == null ? defaultValue : out;
        }

        @Nullable
        public T getIfPresent(int index){
            if(ENABLE_SAFETY) Objects.checkIndex(index, TREE_WIDTH);
            return children[index];
        }

        @NotNull
        public T get(int index){
            if(ENABLE_SAFETY) Objects.checkIndex(index, TREE_WIDTH);
            if(children[index] == null) {
                status |= 1 << index;
                return children[index] = initializer.get();
            }
            return children[index];
        }

        @Nullable
        public T remove(int index){
            if(ENABLE_SAFETY) Objects.checkIndex(index, TREE_WIDTH);
            var out = children[index];
            status &= ~(1 << index);
            children[index] = null;
            return out;
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

    private static class LongTrieIterator implements PrimitiveIterator.OfLong {

        private final int[] childIndex;
        private final BitTreeNode<?>[] nodeAtDepth;

        public LongTrieIterator(LocationTrieSet set) {
            // Initialize our state and the first output
            // Where possible we use getIfPresent as it is more efficient
            childIndex = new int[TREE_DEPTH];
            nodeAtDepth = new BitTreeNode[TREE_DEPTH];
            nodeAtDepth[0] = set.tree;
            for(int i = 0; i < TREE_DEPTH-1; i++){
                childIndex[i] = nodeAtDepth[i].nextSetChild(0);
                nodeAtDepth[i+1] = (BitTreeNode<?>) nodeAtDepth[i].getIfPresent(childIndex[i]);
            }
            childIndex[TREE_DEPTH-1] = nodeAtDepth[TREE_DEPTH-1].nextSetChild(0);
        }

        @Override
        public boolean hasNext() {
            return childIndex[0] != -1;
        }

        @Override
        public long nextLong() {
            // Pack the previously known correct output
            long packed = getPacked();
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
                return packed;
            }
            // Otherwise, descend the tree and update the state to match our path
            if(i != TREE_DEPTH - 1)
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
            }
            return packed;
        }

        private long getPacked() {
            long packed=0;
            for(int i = 0; i < TREE_DEPTH; i++){
                packed |= ((long) childIndex[i]) << (TREE_MASK_LENGTH*(i));
            }
            return packed;
        }
    }
}
