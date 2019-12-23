package net.countercraft.movecraft.compat.v1_13_R2;

import net.minecraft.server.v1_13_R2.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.bukkit.craftbukkit.v1_13_R2.util.HashTreeSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;

public class NextTickProvider {
	private Map<WorldServer,ImmutablePair<HashTreeSet<NextTickListEntry<Block>>,List<NextTickListEntry<Block>>>> tickMap = new HashMap<>();

    private boolean isRegistered(@NotNull WorldServer world){
        return tickMap.containsKey(world);
    }

    @SuppressWarnings("unchecked")
    private void registerWorld(@NotNull WorldServer world){
        List<NextTickListEntry<Block>> g = new ArrayList<>();
        HashTreeSet<NextTickListEntry<Block>> nextTickList = new HashTreeSet<>();
        TickListServer<Block> blockTickListServer = world.getBlockTickList();
        try {
            Field gField = TickListServer.class.getDeclaredField("g");
            gField.setAccessible(true);
            g = (List<NextTickListEntry<Block>>) gField.get(blockTickListServer);
            Field ntlField = TickListServer.class.getDeclaredField("nextTickList");
            ntlField.setAccessible(true);
            nextTickList = (HashTreeSet<NextTickListEntry<Block>>) ntlField.get(blockTickListServer);
        } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException e1) {
            e1.printStackTrace();
        }
        tickMap.put(world, new ImmutablePair<>(nextTickList, g));
    }

    @Nullable
    public NextTickListEntry<Block> getNextTick(@NotNull WorldServer world,@NotNull BlockPosition position){
        if(!isRegistered(world))
            registerWorld(world);
        ImmutablePair<HashTreeSet<NextTickListEntry<Block>>, List<NextTickListEntry<Block>>> listPair = tickMap.get(world);
        if(listPair.left.contains(fakeEntry(position))) {
            for (Iterator<NextTickListEntry<Block>> iterator = listPair.left.iterator(); iterator.hasNext(); ) {
                NextTickListEntry<Block> listEntry = iterator.next();
                if (position.equals(listEntry.a)) {
                    iterator.remove();
                    return listEntry;
                }
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
