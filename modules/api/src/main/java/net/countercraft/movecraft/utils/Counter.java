package net.countercraft.movecraft.utils;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class Counter<T> {
    private final Object2IntMap<T> counter = new Object2IntOpenHashMap<>();

    public Counter(){
        counter.defaultReturnValue(0);
    }

    public int get(T item){
        return counter.getInt(item);
    }

    public void add(T item){
        counter.put(item, counter.getInt(item) + 1);
    }

    public void clear(){
        counter.clear();
    }

    public int size(){
        return counter.size();
    }

    public boolean isEmpty(){
        return counter.isEmpty();
    }
}
