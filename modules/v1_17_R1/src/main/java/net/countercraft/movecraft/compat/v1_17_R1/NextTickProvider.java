package net.countercraft.movecraft.compat.v1_17_R1;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.TickNextTickData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NextTickProvider {

    @Nullable
    public TickNextTickData<?> getNextTick(@NotNull ServerLevel world, @NotNull BlockPos position){
        return null;
    }
    @NotNull
    public Object fakeEntry(@NotNull BlockPos position){
        return new Object(){
            @Override
            public int hashCode() {
                return position.hashCode();
            }
            @Override
            public boolean equals(Object other){
                if (!(other instanceof TickNextTickData)) {
                    return false;
                }
                return position.equals(((TickNextTickData<?>)other).pos);
            }
        };
    }
}
