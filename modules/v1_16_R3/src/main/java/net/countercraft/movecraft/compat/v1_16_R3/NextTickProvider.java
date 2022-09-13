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
                System.out.println(entry.a.getY());
                System.out.println(position.getY());
                return entry;
            }
        }
        return null;
    }

    public void cleanEntry(@NotNull WorldServer world, @NotNull NextTickListEntry entry) {
        List<NextTickListEntry<Block>> ticksRemoved = world.getBlockTickList().a(new StructureBoundingBox(entry.a, entry.a.south().east()), true, true);
        if (!ticksRemoved.isEmpty()) {
            System.out.println("removed some ticks");
        }
    }
}
