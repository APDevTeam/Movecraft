package net.countercraft.movecraft.compat.v1_14_R1;

import net.minecraft.server.v1_14_R1.BlockPosition;
import net.minecraft.server.v1_14_R1.NextTickListEntry;
import net.minecraft.server.v1_14_R1.StructureBoundingBox;
import net.minecraft.server.v1_14_R1.WorldServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NextTickProvider {
    @Nullable
    public NextTickListEntry getNextTick(@NotNull WorldServer world,@NotNull BlockPosition position) {
        BlockPosition position1 = new BlockPosition(position.getX() + 1, position.getY() + 1, position.getZ() + 1);
        for (NextTickListEntry entry : world.getBlockTickList().a(new StructureBoundingBox(position, position1), true, true)) {
            if (position.equals(entry.a)) {
                return entry;
            }
        }
        return null;
    }
}
