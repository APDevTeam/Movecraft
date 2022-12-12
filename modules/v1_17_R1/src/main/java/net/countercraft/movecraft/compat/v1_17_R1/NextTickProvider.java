package net.countercraft.movecraft.compat.v1_17_R1;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerTickList;
import net.minecraft.world.level.TickNextTickData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.logging.Logger;

public class NextTickProvider {

    @Nullable
    public TickNextTickData<?> getNextTick(@NotNull ServerLevel world, @NotNull BlockPos position){
        ServerTickList<Block> tickList = world.getBlockTickList();
        var box = BoundingBox.encapsulatingPositions(List.of(position));
        if(box.isEmpty()){
            return null;
        }
        for(var tick : tickList.fetchTicksInArea(box.get(), false, false)){
            Logger.getAnonymousLogger().info("" + tick);
            return tick;
        }
        return null;
    }
}
