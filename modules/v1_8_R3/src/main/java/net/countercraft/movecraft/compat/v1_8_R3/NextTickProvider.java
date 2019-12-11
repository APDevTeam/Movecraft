package net.countercraft.movecraft.compat.v1_8_R3;


import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.NextTickListEntry;
import net.minecraft.server.v1_8_R3.WorldServer;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.bukkit.craftbukkit.v1_8_R3.util.HashTreeSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.*;

public class NextTickProvider {
    private Map<WorldServer, ImmutablePair<WeakReference<HashTreeSet<NextTickListEntry>>,WeakReference<List<NextTickListEntry>>>> tickMap = new HashMap<>();

    private boolean isRegistered(@NotNull WorldServer world){
        if(!tickMap.containsKey(world))
            return false;
        ImmutablePair<WeakReference<HashTreeSet<NextTickListEntry>>, WeakReference<List<NextTickListEntry>>> listPair = tickMap.get(world);
        return listPair.right.get() != null && listPair.left.get() != null;
    }

    @SuppressWarnings("unchecked")
    private void registerWorld(@NotNull WorldServer world){
        List<NextTickListEntry> V = new ArrayList<>();
        HashTreeSet<NextTickListEntry> M = new HashTreeSet<>();

        try {

            Field VField = WorldServer.class.getDeclaredField("V");
            VField.setAccessible(true);
            V = (List<NextTickListEntry>) VField.get(world);
            Field MField = WorldServer.class.getDeclaredField("M");
            MField.setAccessible(true);
            M = (HashTreeSet<NextTickListEntry>) MField.get(world);
        } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException e1) {
            e1.printStackTrace();
        }
        tickMap.put(world, new ImmutablePair<>(new WeakReference<>(M), new WeakReference<>(V)));
    }

    @Nullable
    @SuppressWarnings("ConstantConditions")
    public NextTickListEntry getNextTick(@NotNull WorldServer world, @NotNull BlockPosition position){
        if(!isRegistered(world))
            registerWorld(world);
        ImmutablePair<WeakReference<HashTreeSet<NextTickListEntry>>, WeakReference<List<NextTickListEntry>>> listPair = tickMap.get(world);
        for(Iterator<NextTickListEntry> iterator = listPair.left.get().iterator(); iterator.hasNext();) {
            NextTickListEntry listEntry = iterator.next();
            if (position.equals(listEntry.a)) {
                iterator.remove();
                return listEntry;
            }
        }
        for(Iterator<NextTickListEntry> iterator = listPair.right.get().iterator(); iterator.hasNext();) {
            NextTickListEntry listEntry = iterator.next();
            if (position.equals(listEntry.a)) {
                iterator.remove();
                return listEntry;
            }
        }
        return null;

    }
}
