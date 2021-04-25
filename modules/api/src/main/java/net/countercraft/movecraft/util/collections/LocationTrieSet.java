package net.countercraft.movecraft.util.collections;

import net.countercraft.movecraft.MovecraftLocation;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Deprecated
public class LocationTrieSet extends AbstractSet<MovecraftLocation> implements Set<MovecraftLocation> {

    private static final int TREE_DEPTH = 16;
    private static final int TREE_MASK_LENGTH = (Long.BYTES * 8)/TREE_DEPTH;

    private final BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<BitTreeNode<LeafNode>>>>>>>>>>>>>>>> tree = new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> new BitTreeNode<>(() -> null))))))))))))))));
    private int size = 0;
    private LeafNode head = null;

    public LocationTrieSet(){}

    public LocationTrieSet(Collection<MovecraftLocation> other){
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
        return suffix.getIfPresent(leafIndex) != null;
    }

    PrimitiveIterator.OfLong longIterator(){
        return new LinkedLongIterator(head);
    }

    @NotNull
    @Override
    public Iterator<MovecraftLocation> iterator() {
        var iter = new LinkedLongIterator(head);
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
        int leafIndex = (int) (packed >>> TREE_MASK_LENGTH * (TREE_DEPTH - 1));

        if(!this.contains(packed)){
            size += 1;
            LeafNode addition = new LeafNode(packed, null, head);
            if (head != null) {
                head.setPrevious(addition);
            }
            head = addition;

            var leaf = this.getPrefixLeaf(packed);
            leaf.set(leafIndex, addition);
            leaf.status |= 1 << leafIndex;
            return true;
        }
        return false;
    }



    @NotNull BitTreeNode<?> commonAncestor(long path){
        BitTreeNode<?> previous;
        BitTreeNode<?> top = tree;
        int i;
        int currentPath = (int) path;
        for(i = 0; i < 8; i++) {
            previous = top;
            top = (BitTreeNode<?>) top.getIfPresent(currentPath & BitTreeNode.TREE_MASK);
            if (top == null) {
                return previous;
            }
            currentPath >>>= TREE_MASK_LENGTH;
        }
        currentPath = (int) (path >>> 32);
        for(; i < TREE_DEPTH - 1; i++) {
            previous = top;
            top = (BitTreeNode<?>) top.getIfPresent(currentPath & BitTreeNode.TREE_MASK);
            if (top == null) {
                return previous;
            }
            currentPath >>>= TREE_MASK_LENGTH;
        }

        return top;
    }

    @Nullable LeafNode getNext(long path){
        BitTreeNode<?> top = commonAncestor(path);
        while (true){
            int childIndex = top.nextSetChild(0);
            if(childIndex == -1){
                return null;
            }
            Object child = top.getIfPresent(childIndex);
            if(child instanceof LeafNode){
                return (LeafNode) child;
            }
            top = (BitTreeNode<?>) child;
        }
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
        LeafNode out = leaf.getIfPresent(leafIndex);
        leaf.remove(leafIndex);
        if(out != null){
            size -= 1;
            if(leaf.nextSetChild(0) == -1){
                this.remove(packed, 0, tree);
            }
            if(head.value == packed){
                head = out.next;
            }
            if(out.previous != null){
                out.previous.setNext(out.next);
            }
            if (out.next != null) {
                out.next.setPrevious(out.previous);
            }
            return true;
        }
        return false;
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
    private BitTreeNode<LeafNode> getPrefixLeafIfPresent(long path){
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

        return (BitTreeNode<LeafNode>) top;
    }

    @NotNull
    private BitTreeNode<LeafNode> getPrefixLeaf(long path){
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

         public int previousSetChild(int end){
             int masked = status & (0xffffffff >>> -(end+1));
             if(masked == 0){
                 return -1;
             }
             return 32 - 1 - Integer.numberOfLeadingZeros(masked);
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
                children[index] = initializer.get();
            }
            return children[index];
        }

        // Should only be called on Trie leaves
        public void set(int index, T value){
            children[index] = value;
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

    private static class LeafNode{
        private final long value;
        @Nullable private LeafNode next,previous;

        private LeafNode(long value, @Nullable LeafNode previous, @Nullable LeafNode next) {
            this.value = value;
            this.next = next;
            this.previous = previous;
        }

        public long getValue(){
            return value;
        }

        @Nullable LeafNode getNext(){
            return next;
        }
        void setNext(@Nullable LeafNode next){
            this.next = next;
        }

        @Nullable LeafNode getPrevious(){
            return previous;
        }

        void setPrevious(@Nullable LeafNode previous){
            this.previous = previous;
        }
    }

    private static class LinkedLongIterator implements PrimitiveIterator.OfLong{

        private LeafNode head;

        private LinkedLongIterator(LeafNode head) {
            this.head = head;
        }

        @Override
        public long nextLong() {
            LeafNode previous = head;
            head = head.next;
            return previous.value;
        }

        @Override
        public boolean hasNext() {
            return head != null;
        }
    }
}
