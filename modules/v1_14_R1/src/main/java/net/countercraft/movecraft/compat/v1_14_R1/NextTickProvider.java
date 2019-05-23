package net.countercraft.movecraft.compat.v1_14_R1;

import net.countercraft.movecraft.utils.Pair;

import net.minecraft.server.v1_14_R1.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;

public class NextTickProvider {
	private Map<WorldServer, Pair<Set<NextTickListEntry>,List<NextTickListEntry>>> tickMap = new HashMap<>();

    private boolean isRegistered(@NotNull WorldServer world){
        return tickMap.containsKey(world);
    }

    @SuppressWarnings("unchecked")
    private void registerWorld(@NotNull WorldServer world){
        List<NextTickListEntry> W = new ArrayList<>();
        Set<NextTickListEntry> nextTickList = new HashSet<>();
        TickListServer<Block> blockTickListServer = world.getBlockTickList();
        try {
            Field gField = TickListServer.class.getDeclaredField("g");
            gField.setAccessible(true);
            W = (List<NextTickListEntry>) gField.get(blockTickListServer);
            Field ntlField = TickListServer.class.getDeclaredField("nextTickList");
            ntlField.setAccessible(true);
            nextTickList = (Set<NextTickListEntry>) ntlField.get(blockTickListServer);
        } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException e1) {
            e1.printStackTrace();
        }
        tickMap.put(world, new Pair<>(nextTickList,W));
    }

    @Nullable
    public NextTickListEntry getNextTick(@NotNull WorldServer world,@NotNull BlockPosition position){
        if(!isRegistered(world))
            registerWorld(world);
        Pair<Set<NextTickListEntry>, List<NextTickListEntry>> listPair = tickMap.get(world);
        for(Iterator<NextTickListEntry> iterator = listPair.getLeft().iterator(); iterator.hasNext();) {
            NextTickListEntry listEntry = iterator.next();
            if (position.equals(listEntry.a)) {
                iterator.remove();
                return listEntry;
            }
        }
        for(Iterator<NextTickListEntry> iterator = listPair.getRight().iterator(); iterator.hasNext();) {
            NextTickListEntry listEntry = iterator.next();
            if (position.equals(listEntry.a)) {
                iterator.remove();
                return listEntry;
            }
        }
        return null;

    }
}
