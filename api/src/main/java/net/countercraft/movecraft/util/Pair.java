package net.countercraft.movecraft.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class Pair<V,K> {
    @NotNull
    private final V v;
    @NotNull private final K k;
    public Pair(@NotNull V v, @NotNull K k){
        this.v = v;

        this.k = k;
    }

    @NotNull
    public K getRight() {
        return k;
    }

    @NotNull
    public V getLeft() {
        return v;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return Objects.equals(v, pair.v) &&
                Objects.equals(k, pair.k);
    }

    @Override
    public int hashCode() {
        return this.k.hashCode() ^ this.v.hashCode();
    }
}