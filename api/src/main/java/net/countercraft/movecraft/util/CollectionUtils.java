package net.countercraft.movecraft.util;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.util.hitboxes.BitmapHitBox;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

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
    public static <E> List<E> filter(@NotNull final List<E> collection, @NotNull final Collection<E> filter){
        final List<E> returnList = new ArrayList<>();
        final HashSet<E> filterSet = new HashSet<>(filter);
        for(int i = 0; i < collection.size(); i++){
            if(!filterSet.contains(collection.get(i))){
                returnList.add(collection.get(i));
            }
        }
        return returnList;
    }

    @NotNull
    @Contract(pure=true)
    @Deprecated
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
    @Deprecated
    public static BitmapHitBox filter(@NotNull final HitBox collection, @NotNull final HitBox filter){
        final BitmapHitBox returnList = new BitmapHitBox();
        int counter = filter.size();
        for(MovecraftLocation object : collection){
            if(counter <= 0 || !filter.contains(object)){
                returnList.add(object);
            } else {
                counter--;
            }
        }
        return returnList;
    }

    private final static MovecraftLocation[] SHIFTS = {
            new MovecraftLocation(0, 0, 1),
//            new MovecraftLocation(0, 1, 0),
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
