package net.countercraft.movecraft.processing.effects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface Effect {
    Effect NONE = new Effect() {
        @Override
        public void run() {
            // No-op
        }

        @Override
        public boolean isAsync() {
            return true;
        }
    };

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
