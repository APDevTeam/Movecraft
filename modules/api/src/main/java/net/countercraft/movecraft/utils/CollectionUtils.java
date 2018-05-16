package net.countercraft.movecraft.utils;

import net.countercraft.movecraft.MovecraftLocation;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;

public class CollectionUtils {
    /**
     * Removes the elements from <code>collection</code> that also exist in <code>filter</code> without modifying either.
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

    @NotNull
    @Contract(pure=true)
    public static Collection<MovecraftLocation> filter(@NotNull final HitBox collection, @NotNull final Collection<MovecraftLocation> filter){
        final Collection<MovecraftLocation> returnList = new HashSet<>();
        final HashSet<MovecraftLocation> filterSet = new HashSet<>(filter);
        for(MovecraftLocation object : collection){
            if(!filterSet.contains(object)){
                returnList.add(object);
            }
        }
        return returnList;
    }
}
