package net.countercraft.movecraft.processing.effects;

@FunctionalInterface
public interface Effect {
    void run();

    default boolean isAsync(){
        return false;
    }
}
