package net.countercraft.movecraft.compat.v1_11_R1;

import net.minecraft.server.v1_11_R1.BlockPosition;
import net.minecraft.server.v1_11_R1.NextTickListEntry;
import net.minecraft.server.v1_11_R1.WorldServer;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.bukkit.craftbukkit.v1_11_R1.util.HashTreeSet;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class NextTickProvider {
    private Map<WorldServer,ImmutablePair<HashTreeSet<NextTickListEntry>,List<NextTickListEntry>>> tickMap = new HashMap<>();

    private boolean isRegistered(@NotNull WorldServer world){
        return tickMap.containsKey(world);
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
        tickMap.put(world, new ImmutablePair<>(nextTickList,U));
    }

    @Nullable
    public NextTickListEntry getNextTick(@NotNull WorldServer world,@NotNull BlockPosition position){
        if(!isRegistered(world))
            registerWorld(world);
        ImmutablePair<HashTreeSet<NextTickListEntry>, List<NextTickListEntry>> listPair = tickMap.get(world);
        if(listPair.left.contains(fakeEntry(position))) {
            for (Iterator<NextTickListEntry> iterator = listPair.left.iterator(); iterator.hasNext(); ) {
                NextTickListEntry listEntry = iterator.next();
                if (position.equals(listEntry.a)) {
                    iterator.remove();
                    return listEntry;
                }
            }
        }
        for(Iterator<NextTickListEntry> iterator = listPair.right.iterator(); iterator.hasNext();) {
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
