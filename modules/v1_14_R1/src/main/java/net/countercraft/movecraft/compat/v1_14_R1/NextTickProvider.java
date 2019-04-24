package net.countercraft.movecraft.compat.v1_14_R1;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.bukkit.craftbukkit.v1_14_R1.util.HashTreeSet;

import net.minecraft.server.v1_14_R1.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;

public class NextTickProvider {
	private Map<WorldServer,ImmutablePair<HashTreeSet<NextTickListEntry>,List<NextTickListEntry>>> tickMap = new HashMap<>();

    private boolean isRegistered(@NotNull WorldServer world){
        return tickMap.containsKey(world);
    }

    @SuppressWarnings("unchecked")
    private void registerWorld(@NotNull WorldServer world){
        List<NextTickListEntry> W = new ArrayList<>();
        HashTreeSet<NextTickListEntry> nextTickList = new HashTreeSet<>();
        TickListServer<Block> blockTickListServer = world.getBlockTickList();
        try {
            Field gField = TickListServer.class.getDeclaredField("g");
            gField.setAccessible(true);
            W = (List<NextTickListEntry>) gField.get(blockTickListServer);
            Field ntlField = TickListServer.class.getDeclaredField("nextTickList");
            ntlField.setAccessible(true);
            nextTickList = (HashTreeSet<NextTickListEntry>) ntlField.get(blockTickListServer);
        } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException e1) {
            e1.printStackTrace();
        }
        tickMap.put(world, new ImmutablePair<>(nextTickList,W));
    }

    @Nullable
    public NextTickListEntry getNextTick(@NotNull WorldServer world,@NotNull BlockPosition position){
        if(!isRegistered(world))
            registerWorld(world);
        ImmutablePair<HashTreeSet<NextTickListEntry>, List<NextTickListEntry>> listPair = tickMap.get(world);
        for(Iterator<NextTickListEntry> iterator = listPair.left.iterator(); iterator.hasNext();) {
            NextTickListEntry listEntry = iterator.next();
            if (position.equals(listEntry.a)) {
                iterator.remove();
                return listEntry;
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
}
