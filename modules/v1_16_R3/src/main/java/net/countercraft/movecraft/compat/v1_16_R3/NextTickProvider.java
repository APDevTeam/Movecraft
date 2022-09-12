package net.countercraft.movecraft.compat.v1_16_R3;

import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.NextTickListEntry;
import net.minecraft.server.v1_16_R3.StructureBoundingBox;
import net.minecraft.server.v1_16_R3.WorldServer;
import org.bukkit.craftbukkit.libs.org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class NextTickProvider {
    
    private Map<WorldServer,ImmutablePair<WeakReference<TreeSet<NextTickListEntry>>,WeakReference<List<NextTickListEntry>>>> tickMap = new HashMap<>();

    @Nullable
    @SuppressWarnings("ConstantConditions")
    public NextTickListEntry getNextTick(@NotNull WorldServer world,@NotNull BlockPosition position) {
        BlockPosition position1 = new BlockPosition(position.getX() + 1, position.getY() + 1, position.getZ() + 1);
        for (NextTickListEntry entry : world.getBlockTickList().a(new StructureBoundingBox(position, position1), false, true)) {
            if (position.equals(entry.a)) {
                return entry;
            }
        }
        return null;
    }
}
