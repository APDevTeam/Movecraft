package net.countercraft.movecraft.compat.v1_10_R1;

import net.minecraft.server.v1_10_R1.BlockPosition;
import net.minecraft.server.v1_10_R1.NextTickListEntry;
import net.minecraft.server.v1_10_R1.WorldServer;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.bukkit.craftbukkit.v1_10_R1.util.HashTreeSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class NextTickProvider {
    private Map<WorldServer,ImmutablePair<WeakReference<HashTreeSet<NextTickListEntry>>,WeakReference<List<NextTickListEntry>>>> tickMap = new HashMap<>();

    private boolean isRegistered(@NotNull WorldServer world){
        if(!tickMap.containsKey(world))
            return false;
        ImmutablePair<WeakReference<HashTreeSet<NextTickListEntry>>, WeakReference<List<NextTickListEntry>>> listPair = tickMap.get(world);
        return listPair.right.get() != null && listPair.left.get() != null;
    }

    @SuppressWarnings("unchecked")
    private void registerWorld(@NotNull WorldServer world){
        List<NextTickListEntry> U = new ArrayList<>();
        HashTreeSet<NextTickListEntry> nextTickList = new HashTreeSet<>();

        try {

            Field UField = WorldServer.class.getDeclaredField("U");
            UField.setAccessible(true);
            U = (List<NextTickListEntry>) UField.get(world);
            Field nextTickListField = WorldServer.class.getDeclaredField("nextTickList");
            nextTickListField.setAccessible(true);
            nextTickList = (HashTreeSet<NextTickListEntry>) nextTickListField.get(world);
        } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException e1) {
            e1.printStackTrace();
        }
        tickMap.put(world, new ImmutablePair<>(new WeakReference<>(nextTickList), new WeakReference<>(U)));
    }

    @Nullable
    @SuppressWarnings("ConstantConditions")
    public NextTickListEntry getNextTick(@NotNull WorldServer world,@NotNull BlockPosition position){
        if(!isRegistered(world))
            registerWorld(world);
        ImmutablePair<WeakReference<HashTreeSet<NextTickListEntry>>, WeakReference<List<NextTickListEntry>>> listPair = tickMap.get(world);
        if(listPair.left.get().contains(fakeEntry(position))) {
            for (Iterator<NextTickListEntry> iterator = listPair.left.get().iterator(); iterator.hasNext(); ) {
                NextTickListEntry listEntry = iterator.next();
                if (position.equals(listEntry.a)) {
                    iterator.remove();
                    return listEntry;
                }
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

    @NotNull
    public Object fakeEntry(@NotNull BlockPosition position){
        return new Object(){
            @Override
            public int hashCode() {
                return position.hashCode();
            }
        };
    }
}
