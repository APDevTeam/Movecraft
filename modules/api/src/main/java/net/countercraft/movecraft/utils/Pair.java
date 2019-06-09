package net.countercraft.movecraft.utils;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a single immutable pair of objects
 * @param <L> The left object
 * @param <R> The right object
 */
public final class Pair<L,R> implements Serializable, Map.Entry<L,R>{
    private final UUID id = UUID.randomUUID();
    private final L left;
    private final R right;

    public Pair(L left, R right){
        this.left = left;
        this.right = right;
    }

    /**
     *
     * @return
     */
    public L getLeft() {
        return left;
    }

    public R getRight() {
        return right;
    }


    @Override
    public L getKey() {
        return left;
    }

    @Override
    public R getValue() {
        return right;
    }

    @Override
    public R setValue(R value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Pair)){
            return false;
        }
        Pair pair = (Pair) obj;
        return getLeft().equals(pair.getLeft()) && getRight().equals(pair.getRight());
    }

    @Override
    public int hashCode() {
        return Objects.hash(left,right);
    }

    @Override
    public String toString() {
        return "Pair{Left: " + getLeft().toString() + ", Right: " + getRight().toString() + "}";
    }

    @Override
    public Pair clone(){
        Object clone = null;
        try {
            clone = super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return (Pair) clone;
    }
}
