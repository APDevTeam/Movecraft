package net.countercraft.movecraft.util;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class Counter<T> {
    private final Object2IntMap<T> counter = new Object2IntOpenHashMap<>();

    public Counter() {
        counter.defaultReturnValue(0);
    }

    public Counter(@NotNull Counter<T> other) {
        this.counter.putAll(other.counter);
    }

    public static <T> Counter<T> add(Counter<T> left, Counter<T> right){
        var out = new Counter<>(left);
        out.add(right);

        return out;
    }

    public int get(T item) {
        return counter.getInt(item);
    }

    public void set(T item, int count) {
        counter.put(item, count);
    }

    public void add(T item, int count) {
        counter.put(item, counter.getInt(item) + count);
    }

    public void add(T item) {
        add(item, 1);
    }

    public void clear() {
        counter.clear();
    }

    public void clear(T item) {
        counter.removeInt(item);
    }

    public int size() {
        return counter.size();
    }

    public boolean isEmpty() {
        return counter.isEmpty();
    }

    public Set<T> getKeySet() {
        return counter.keySet();
    }

    public ObjectSet<Object2IntMap.Entry<T>> getEntrySet(){
        return counter.object2IntEntrySet();
    }

    public void add(@NotNull Counter<T> other) {
        other.getKeySet().forEach(key -> counter.put(key, other.get(key)));
    }
}
