package net.countercraft.movecraft.api.utils;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;

public class CollectionUtils {
    /**
     * Removes the elements from <code>collection</code> that also exist in <code>filter</code> without modifying either.
     * O(1) runtime.
     * @param <E> the element type
     * @return a <code>Collection</code> containing all the elements of <code>collection</code> except those in <code>filter</code>
     */
    @NotNull
    @Contract(pure=true)
    public static <E> Collection<E> filter(@NotNull final Collection<E> collection, @NotNull final Collection<E> filter){
        final Collection<E> returnList = new HashSet<>();
        final HashSet<E> filterSet = new HashSet<>(filter);
        for(E object : collection){
            if(!filterSet.contains(object)){
                returnList.add(object);
            }
        }
        return returnList;
    }
}
