package net.countercraft.movecraft.compat.v1_16_R3;

import net.countercraft.movecraft.util.hitboxes.HitBox;
import net.minecraft.server.v1_16_R3.Block;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.NextTickListEntry;
import net.minecraft.server.v1_16_R3.StructureBoundingBox;
import net.minecraft.server.v1_16_R3.WorldServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class NextTickProvider {

    @Nullable
    public NextTickListEntry getNextTick(@NotNull WorldServer world,@NotNull BlockPosition position) {
        BlockPosition position1 = new BlockPosition(position.getX() + 1, position.getY(), position.getZ() + 1);
        for (NextTickListEntry entry : world.getBlockTickList().a(new StructureBoundingBox(position, position1), true, false)) {
            if (position.equals(entry.a)) {
                return entry;
            }
        }
        return null;
    }
}
