package net.countercraft.movecraft.compat.v1_16_R1;

import net.minecraft.server.v1_16_R1.BlockPosition;
import net.minecraft.server.v1_16_R1.NextTickListEntry;
import net.minecraft.server.v1_16_R1.WorldServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NextTickProvider {

    @Nullable
    public NextTickListEntry<?> getNextTick(@NotNull WorldServer world,@NotNull BlockPosition position){
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
