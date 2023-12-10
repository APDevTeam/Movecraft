package net.countercraft.movecraft.compat.v1_20_R3;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.ScheduledTick;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Queue;

public class NextTickProvider {

    @Nullable
    public ScheduledTick<?> getNextTick(@NotNull ServerLevel world, @NotNull BlockPos position){
        LevelTicks<Block> tickList = world.getBlockTicks();
        var box = BoundingBox.encapsulatingPositions(List.of(position));
        if(box.isEmpty()){
            return null;
        }
        Queue<ScheduledTick<?>> toRunThisTick;
        try {
            Field toRunThisTickField = LevelTicks.class.getDeclaredField("g"); // g is obfuscated toRunThisTick
            toRunThisTickField.setAccessible(true);
            toRunThisTick = (Queue<ScheduledTick<?>>) toRunThisTickField.get(tickList);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
        for (var iter = toRunThisTick.iterator(); iter.hasNext(); ) {
            var next = iter.next();
            if (!next.pos().equals(position)) {
                continue;
            }
            iter.remove();
            return next;
        }
        return null;
    }
}
