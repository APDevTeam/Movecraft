package net.countercraft.movecraft.processing.effects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface Effect {
    void run();

    default boolean isAsync(){
        return false;
    }

    default @NotNull
    Effect andThen(@Nullable Effect chain){
        if(chain == null){
            return this;
        }
        return () -> {
            this.run();
            chain.run();
        };
    }
}
