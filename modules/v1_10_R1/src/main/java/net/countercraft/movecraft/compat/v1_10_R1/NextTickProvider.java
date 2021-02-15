package net.countercraft.movecraft.compat.v1_10_R1;

import net.minecraft.server.v1_10_R1.BlockPosition;
import net.minecraft.server.v1_10_R1.NextTickListEntry;
import net.minecraft.server.v1_10_R1.StructureBoundingBox;
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
    @Nullable
    @SuppressWarnings("ConstantConditions")
    public NextTickListEntry getNextTick(@NotNull WorldServer world,@NotNull BlockPosition position){
        StructureBoundingBox box = new StructureBoundingBox(position, position.a(1,1,1));
        List<NextTickListEntry> entries=world.a(box, true);
        return entries.get(0);

    }
}
