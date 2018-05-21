package net.countercraft.movecraft.utils;

import net.countercraft.movecraft.MovecraftLocation;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;

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

    @NotNull
    @Contract(pure=true)
    public static HitBox filter(@NotNull final HitBox collection, @NotNull final HitBox filter){
        final MutableHitBox returnList = new HashHitBox();
        final MutableHitBox filterBox = new HashHitBox();
        filterBox.addAll(filter);
        for(MovecraftLocation object : collection){
            if(!filterBox.contains(object)){
                returnList.add(object);
            }
        }
        return returnList;
    }

    private final static MovecraftLocation[] SHIFTS = {
            new MovecraftLocation(0, 0, 1),
            new MovecraftLocation(0, 1, 0),
            new MovecraftLocation(1, 0 ,0),
            new MovecraftLocation(0, 0, -1),
            new MovecraftLocation(0, -1, 0),
            new MovecraftLocation(-1, 0, 0)};
    /**
     * finds the axial neighbors to a location. Neighbors are defined as locations that exist within one meter of a given
     * location
     * @param location the location to search for neighbors
     * @return an iterable set of neighbors to the given location
     */
    @NotNull
    @Contract(pure = true)
    public static Iterable<MovecraftLocation> neighbors(@NotNull HitBox hitbox, @NotNull MovecraftLocation location){
        if(hitbox.isEmpty()){
            return Collections.emptyList();
        }
        final List<MovecraftLocation> neighbors = new ArrayList<>(6);
        for(MovecraftLocation test : SHIFTS){
            if(hitbox.contains(location.add(test))){
                neighbors.add(location.add(test));
            }
        }
        return neighbors;
    }
}
