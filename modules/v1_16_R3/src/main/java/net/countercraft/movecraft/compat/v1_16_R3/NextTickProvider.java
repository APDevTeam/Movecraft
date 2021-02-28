package net.countercraft.movecraft.compat.v1_16_R3;

import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.NextTickListEntry;
import net.minecraft.server.v1_16_R3.WorldServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
