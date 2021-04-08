package net.countercraft.movecraft.util.collections;

import org.jetbrains.annotations.NotNull;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.PrimitiveIterator;
import java.util.function.LongFunction;
import java.util.function.ToLongFunction;

public class PackableTrie<T> extends AbstractSet<T>{

    private final ToLongFunction<T> packFunction;
    private final LongFunction<T> unpackFunction;
    private final Class<T> clazz;
    private final LocationTrieSet backing = new LocationTrieSet();

    public PackableTrie(ToLongFunction<T> packFunction, LongFunction<T> unpackFunction, Class<T> clazz){
        this.packFunction = packFunction;
        this.unpackFunction = unpackFunction;
        this.clazz = clazz;
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        PrimitiveIterator.OfLong iter = backing.longIterator();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public T next() {
                return unpackFunction.apply(iter.next());
            }
        };
    }

    @Override
    public int size() {
        return backing.size();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object o){
        if(clazz.isInstance(o)){
            return backing.contains(packFunction.applyAsLong((T)o));
        }
        return false;
    }

    @Override
    public boolean add(T item){
        return backing.add(packFunction.applyAsLong(item));
    }
}
