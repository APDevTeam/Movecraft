package net.countercraft.movecraft.compat.v1_17_R1;

import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.NextTickListEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NextTickProvider {

    @Nullable
    public NextTickListEntry<?> getNextTick(@NotNull WorldServer world, @NotNull BlockPosition position){
        return null;
    }
    @NotNull
    public Object fakeEntry(@NotNull BlockPosition position){
        return new Object(){
            @Override
            public int hashCode() {
                return position.hashCode();
            }
            @Override
            public boolean equals(Object other){
                if (!(other instanceof NextTickListEntry)) {
                    return false;
                }
                return position.equals(((NextTickListEntry<?>)other).a);
            }
        };
    }
}
