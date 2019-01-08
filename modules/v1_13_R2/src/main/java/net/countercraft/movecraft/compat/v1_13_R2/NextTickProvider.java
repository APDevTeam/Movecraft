package net.countercraft.movecraft.compat.v1_13_R2;

import net.minecraft.server.v1_13_R2.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.bukkit.craftbukkit.v1_13_R2.util.HashTreeSet;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class NextTickProvider {
	private Map<WorldServer,ImmutablePair<HashTreeSet<NextTickListEntry>,List<NextTickListEntry<Block>>>> tickMap = new HashMap<>();

    private boolean isRegistered(@NotNull WorldServer world){
        return tickMap.containsKey(world);
    }

    @SuppressWarnings("unchecked")
    private void registerWorld(@NotNull WorldServer world){
        List<NextTickListEntry<Block>> W = new ArrayList<>();
        HashTreeSet<NextTickListEntry> nextTickList = new HashTreeSet<>();
        Chunk chunk = world.getChunkProvider().a().iterator().next();

        try {
            W = world.getBlockTickList().a(chunk, false);
            Field ntlField = TickListServer.class.getDeclaredField("nextTickList");
            ntlField.setAccessible(true);
            ntlField.get(world.getBlockTickList());
        } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException e1) {
            e1.printStackTrace();
        }
        tickMap.put(world, new ImmutablePair<>(nextTickList,W));
    }

    @Nullable
    public NextTickListEntry getNextTick(@NotNull WorldServer world,@NotNull BlockPosition position){
        if(!isRegistered(world))
            registerWorld(world);
        ImmutablePair<HashTreeSet<NextTickListEntry>, List<NextTickListEntry<Block>>> listPair = tickMap.get(world);
        for(Iterator<NextTickListEntry> iterator = listPair.left.iterator(); iterator.hasNext();) {
            NextTickListEntry listEntry = iterator.next();
            if (position.equals(listEntry.a)) {
                iterator.remove();
                return listEntry;
            }
        }
        for(Iterator<NextTickListEntry<Block>> iterator = listPair.right.iterator(); iterator.hasNext();) {
            NextTickListEntry listEntry = iterator.next();
            if (position.equals(listEntry.a)) {
                iterator.remove();
                return listEntry;
            }
        }
        return null;

    }
}
