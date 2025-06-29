package net.countercraft.movecraft.compat.v1_21_1;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ScheduledTick;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

public class NextTickProvider {

    @Nullable
    public ScheduledTick<?> getNextTick(@NotNull ServerLevel world, @NotNull BlockPos position){
        LevelChunkTicks<Block> tickList = (LevelChunkTicks<Block>) world
                .getChunk(position)
                .getBlockTicks();

        var box = BoundingBox.encapsulatingPositions(List.of(position));
        if(box.isEmpty()){
            return null;
        }

        Stream<ScheduledTick<Block>> ticks = tickList.getAll();

        for (var iter = ticks.iterator(); iter.hasNext(); ) {
            var next = iter.next();
            if (!next.pos().equals(position)) {
                continue;
            }
            return next;
        }
        return null;
    }
}
