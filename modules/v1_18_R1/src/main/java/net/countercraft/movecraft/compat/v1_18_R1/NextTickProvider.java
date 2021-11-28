package net.countercraft.movecraft.compat.v1_18_R1;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.logging.Logger;

public class NextTickProvider {

    @Nullable
    public TickNextTickData<?> getNextTick(@NotNull ServerLevel world, @NotNull BlockPos position){
        var tickList = world.getBlockTicks();
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
