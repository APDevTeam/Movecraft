package net.countercraft.movecraft.utils;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

public final class Pair<L,R> implements Serializable, Map.Entry<L,R> {
    private final UUID id = UUID.randomUUID();
    private final L left;
    private final R right;

    public Pair(L left, R right){
        this.left = left;
        this.right = right;
    }

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
        return getLeft().equals(pair.getLeft())&&getRight().equals(pair.getRight());
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }


}
